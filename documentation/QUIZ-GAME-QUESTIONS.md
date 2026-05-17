# Quiz Game — Question Generation Rules

This document describes how [docs/quiz-game.html](../docs/quiz-game.html) builds quiz questions from the LTS flag catalogs in `docs/json/`.

## Session and data

| Rule | Value |
|------|--------|
| LTS versions | Java **8, 11, 17, 21, 25** only |
| Questions per game | **10** |
| Pass threshold | **8/10** correct |
| Data source | `json/java-{version}.json` for each LTS release |

## Index building (before any question)

1. **Flag nodes only** — Only nodes with `type === "flag"` and a non-empty `description` enter the pool.
2. **Parent grouping** — Flags are grouped by their parent category node (from edges where the target is a flag). Ungrouped flags use `"_ungrouped"`.
3. **Cross-version tracking** — Each flag is keyed by `flagId` so the same logical flag can appear in multiple JDK catalogs.
4. **Per-version lists** — `byVersion`, `parentGroupsByVersion`, and a flat `allFlags` list are built for generators.

## Quiz assembly (`generateQuiz`)

1. **Three generators**, chosen uniformly at random each attempt:
   - `buildVersionPresenceQuestion`
   - `buildWhichVersionQuestion`
   - `buildDescriptionQuestion`
2. **Repeat until 10 valid questions** or **300 attempts** (failed generators return `null` and are skipped).
3. **No guarantee of type mix** — Types are random per slot, not balanced (e.g. not forced 3/3/4).
4. **No duplicate slots in a deck** — Each accepted question must have a unique dedup key (see below). Collisions are discarded and the attempt does not count toward the 10 questions.

### Dedup keys (`questionDedupKey`)

A `Set` tracks keys for questions already in the current deck. Format: `` `${type}|${flagId}|${scope}` ``.

| Type | `flagId` | `scopeVersion` | Example key |
|------|----------|----------------|-------------|
| `versionPresence` | Flag under test | Target LTS in the prompt | `versionPresence|UseG1GC|17` |
| `whichVersion` | Flag under test | *(empty)* | `whichVersion|UseG1GC|` |
| `description` | Correct flag | LTS catalog used for sibling pool | `description|UseG1GC|25` |

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

## Presentation rules (all types)

- **4 multiple-choice options** per question (binary for version presence; four Java versions or four descriptions for the other types).
- **Flag text** shown in a monospace block; options escaped with `escapeHtml`.
- **Shuffle** uses Fisher–Yates on arrays; **pickRandom** is uniform.

## Scoring and replay

- One click per question; correct if `selectedIndex === correctIndex`.
- **Play again** rebuilds a new random set with the same index (same loaded JSON).
- If fewer than 10 questions after generation, show: “Could not generate enough questions.”
- On failure, the result screen lists incorrect answers with your choice vs. the correct option.

## Practical implications

- **Description questions** only use flags that share a category with at least three other flags in that JDK, and only when at least four **different** descriptions exist in that sibling pool (avoids unanswerable prompts such as `-XX:JavaPriority3_To_OSPriority=<n>` with four identical choices).
- **Which-version** accepts any LTS option where the flag is present; flags that appear in all five LTS catalogs never appear in this type.
- **Version presence** “absent” uses a flag from *some* catalog that is missing in the asked version—not a synthetic flag name.
- **Dedup** prevents repeating the same `(type, flagId, scopeVersion)` in one game; unrelated types or scopes for the same flag are still allowed.

## Source references

| Constant / function | Location in `docs/quiz-game.html` |
|---------------------|-----------------------------------|
| `LTS_VERSIONS`, `TOTAL_QUESTIONS`, `WIN_THRESHOLD` | ~463–465 |
| `parseGraph`, `buildFlagIndex` | ~486–551 |
| `buildVersionPresenceQuestion` | ~553–581 |
| `buildWhichVersionQuestion` | ~583–616 |
| `buildDescriptionQuestion` | ~618–640 |
| `questionDedupKey` | ~642–645 |
| `generateQuiz` | ~647–668 |
