# Quiz Game — Question Generation Rules

This document describes how [docs/quiz-game.html](../docs/quiz-game.html) builds quiz questions from the LTS flag catalogs in `docs/json/`.

## Session and data

| Rule | Value |
|------|--------|
| LTS versions | Java **8, 11, 17, 21, 25** only |
| Questions per game | **20** |
| Pass threshold | **70%** correct (**14/20**) |
| Data source | `json/java-{version}.json` for each LTS release |

## Index building (before any question)

1. **Flag nodes only** — Only nodes with `type === "flag"` and a non-empty `description` enter the pool.
2. **Parent grouping** — Flags are grouped by their parent category node (from edges where the target is a flag). Ungrouped flags use `"_ungrouped"`.
3. **Cross-version tracking** — Each flag is keyed by `flagId` so the same logical flag can appear in multiple JDK catalogs.
4. **Per-version lists** — `byVersion`, `parentGroupsByVersion`, and a flat `allFlags` list are built for generators.

## Quiz assembly (`generateQuiz`)

1. **Balanced type quotas** per game (20 questions total):
   | Type | Count |
   |------|-------|
   | `flagCount` | **1** (always) |
   | Each of the other **5** types | **3 or 4** (base 2 each + 9 extra slots → four types at 4, one at 3 → 1 + 19 = 20) |
2. **Slot order** — `buildBalancedQuizPlan()` shuffles the 20 type slots so questions are not grouped by type.
3. **Per-slot generation** — For each slot, call the matching builder up to **50** times until a valid question passes dedup; if any slot fails, the deck is incomplete.
4. **No duplicate slots in a deck** — Each accepted question must have a unique dedup key (see below). Collisions are discarded and the attempt does not count toward that slot.

### Dedup keys (`questionDedupKey`)

A `Set` tracks keys for questions already in the current deck. Format: `` `${type}|${flagId}|${scope}` ``.

| Type | `flagId` | `scopeVersion` | Example key |
|------|----------|----------------|-------------|
| `versionPresence` | Flag under test | Target LTS in the prompt | `versionPresence|UseG1GC|17` |
| `whichVersion` | Flag under test | *(empty)* | `whichVersion|UseG1GC|` |
| `description` | Correct flag | LTS catalog used for sibling pool | `description|UseG1GC|25` |
| `flagFromDescription` | Correct flag | LTS catalog used for sibling pool | `flagFromDescription|UseG1GC|25` |
| `firstLts` | Flag under test | *(empty)* | `firstLts|UseG1GC|` |
| `flagCount` | `_catalog` (synthetic) | Target LTS in the prompt | `flagCount|_catalog|17` |

The same `flagId` may appear in **different** question types or **different** `scopeVersion` values (e.g. version check on Java 8 and description on Java 25). It cannot appear twice with the same type and scope (e.g. two “which LTS includes this flag?” questions for the same flag).

---

## Type 1: Version check (`versionPresence`)

**UI label:** Version check  
**Prompt:** “Is this flag available in **Java {version}**?”

| Rule | Detail |
|------|--------|
| Target version | Random LTS (8, 11, 17, 21, 25) |
| Yes/No balance | 50% “present”, 50% “absent” |
| If **present** | Pick a random flag from that version’s catalog |
| If **absent** | Pick a random flag whose `flagId` is **not** in that version |
| Options | `["Yes", "No"]` shuffled |
| Abort | `null` if the chosen version has no flags, or no absent flags exist |

---

## Type 2: LTS release (`whichVersion`)

**UI label:** LTS release  
**Prompt:** “Name **one** LTS release that includes this flag.”

| Rule | Detail |
|------|--------|
| Flag pool | `flagId`s present in **at least one** LTS and **absent from at least one** LTS (flags in all five catalogs are skipped) |
| Correct answer | **Any** LTS version where that `flagId` exists; grading checks the selected option against `correctVersions` |
| Wrong answers | Up to 3 LTS versions where the flag is **not** present (never other versions that also include the flag) |
| Present options | Up to `4 − wrong count` LTS versions where the flag exists (shuffled into the option list) |
| Options | Up to four labels: `Java {n}` — mix of present + absent versions, all shuffled |
| Review text | One label if unique; otherwise `Any of: Java 8, Java 11, …` |
| Abort | `null` if no eligible `flagId`, or fewer than two options |

---

## Type 3: Flag description (`description`)

**UI label:** Flag description  
**Prompt:** “Which description matches this JVM flag?”

| Rule | Detail |
|------|--------|
| Scope version | Random LTS |
| Sibling pool | Parent group must have **≥ 4 flags** in that version **and** **≥ 4 distinct** `description` values among those flags |
| Correct flag | Random flag from that group |
| Distractors | 3 other flags from the **same parent group** (siblings) |
| Options | Four descriptions (1 correct + 3 distractors), shuffled |
| Unique options | All four option texts must be **distinct**; discard if any duplicate (e.g. siblings sharing “Map Java priorities to OS priorities.”) |
| Display | Shows the **correct** flag’s `flag` string |
| Abort | `null` if no eligible group, or if the four chosen descriptions are not all unique |

---

## Type 4: Flag name (`flagFromDescription`)

**UI label:** Flag name  
**Prompt:** “Which JVM flag matches this description?”

| Rule | Detail |
|------|--------|
| Scope version | Random LTS |
| Sibling pool | Same eligibility as **Type 3** (`pickEligibleSiblingGroup`) |
| Correct flag | Random flag from that group |
| Distractors | 3 sibling `flag` strings |
| Options | Four flag names, shuffled |
| Unique options | All four `flag` strings must be **distinct** |
| Display | Shows the **correct** flag’s `description` in the monospace block |
| Abort | `null` if no eligible group, or if the four chosen flag names are not all unique |

---

## Type 5: First LTS (`firstLts`)

**UI label:** First LTS  
**Prompt:** “In which LTS release did this flag **first** appear?”

| Rule | Detail |
|------|--------|
| Flag pool | Any `flagId` present in at least one LTS catalog |
| Correct answer | **Minimum** LTS version number where the flag exists |
| Wrong answers | 3 other LTS version labels (may include versions where the flag also exists later) |
| Options | Four `Java {n}` labels, shuffled |
| Abort | `null` if no eligible flag |

---

## Type 6: Catalog size (`flagCount`)

**UI label:** Catalog size  
**Prompt:** “How many JVM flags are cataloged for **Java {version}** on this site?”

| Rule | Detail |
|------|--------|
| Target version | Random LTS (8, 11, 17, 21, 25) |
| Correct answer | Count of flag nodes with a non-empty `description` in that version’s catalog (same pool as index building) |
| Wrong answers | Flag counts from **three other** LTS catalogs (each LTS has a distinct count, so four unique numeric options) |
| Options | Four numbers as strings (e.g. `753`, `698`), shuffled |
| Display | No flag monospace block (prompt only) |
| Abort | `null` if the version has no flags or fewer than three distinct distractor counts |

---

## Presentation rules (all types)

- **4 multiple-choice options** per question (binary for version presence; four Java versions, descriptions, flag names, or numbers for the other types).
- **Monospace block** — Flag string for most types; description text for `flagFromDescription`; omitted for `flagCount`.
- Options escaped with `escapeHtml`.
- **Shuffle** uses Fisher–Yates on arrays; **pickRandom** is uniform.

## Scoring and replay

- Pass when `score >= WIN_THRESHOLD`, where `WIN_THRESHOLD = ceil(TOTAL_QUESTIONS × 70 / 100)` (14 for 20 questions).
- One click per question; correct if `selectedIndex === correctIndex`, except `whichVersion` (any present LTS option is correct).
- **Play again** rebuilds a new random set with the same index (same loaded JSON).
- If fewer than 20 questions after generation, show: “Could not generate enough questions.”
- On failure, the result screen lists incorrect answers with your choice vs. the correct option.

## Practical implications

- **Description / flag name** questions share the same sibling-pool rules (≥ 4 flags, ≥ 4 distinct descriptions in the group).
- **Which-version** accepts any LTS option where the flag is present; flags that appear in all five LTS catalogs never appear in this type.
- **First LTS** has a single correct answer (earliest cataloged LTS); later releases where the flag also exists are plausible distractors.
- **Version presence** “absent” uses a flag from *some* catalog that is missing in the asked version—not a synthetic flag name.
- **Dedup** prevents repeating the same `(type, flagId, scopeVersion)` in one game; unrelated types or scopes for the same flag are still allowed.
- **Catalog size** appears **once** per game by design; dedup also caps at one per LTS version (`flagCount|_catalog|{version}`).
- **Balanced mix** — Every game includes exactly one catalog-size question; the other five types share the remaining 19 slots (four types at 4 questions, one at 3).

## Source references

| Constant / function | Location in `docs/quiz-game.html` |
|---------------------|-----------------------------------|
| `LTS_VERSIONS`, `TOTAL_QUESTIONS`, `PASS_PERCENT`, `WIN_THRESHOLD` | ~464–467 |
| `TYPE_LABELS` | ~469–476 |
| `parseGraph`, `buildFlagIndex` | ~489–552 |
| `buildVersionPresenceQuestion` | ~554–583 |
| `buildWhichVersionQuestion` | ~600–631 |
| `pickEligibleSiblingGroup`, `buildDescriptionQuestion` | ~633–669 |
| `buildFlagFromDescriptionQuestion` | ~671–699 |
| `buildFirstLtsQuestion` | ~701–727 |
| `buildFlagCountQuestion` | ~729–755 |
| `buildBalancedQuizPlan`, `questionDedupKey`, `generateQuiz` | ~757–819 |
