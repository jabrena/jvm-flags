# Quiz Game — Question Generation Rules

This document describes how [docs/quiz-game.html](../docs/quiz-game.html) builds quiz questions from the LTS flag catalogs in `docs/json/`.

The quiz is scoped to **Java 25**: flag text, descriptions, and sibling pools come from the Java 25 catalog so learners practice flags they can use on the current LTS. All five LTS JSON files are still loaded for version-check “absent” flags and catalog-size distractors.

## Session and data

| Rule | Value |
|------|--------|
| Quiz scope | **Java 25** (`QUIZ_SCOPE_VERSION`) |
| LTS versions loaded | Java **8, 11, 17, 21, 25** (for index and cross-version logic) |
| Questions per game | **20** |
| Pass threshold | **70%** correct (**14/20**) |
| Data source | `json/java-{version}.json` for each LTS release |
| Question types | **4** — version check, flag description, flag name, catalog size |

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
   | Each of the other **3** types | **6 or 7** (base 2 each + 13 extra slots → two types at 7, one at 5 → 1 + 19 = 20) |
2. **Slot order** — `buildBalancedQuizPlan()` shuffles the 20 type slots so questions are not grouped by type.
3. **Per-slot generation** — For each slot, call the matching builder up to **50** times until a valid question passes dedup; if any slot fails, the deck is incomplete.
4. **No duplicate slots in a deck** — Each accepted question must have a unique dedup key (see below). Collisions are discarded and the attempt does not count toward that slot.

### Dedup keys (`questionDedupKey`)

A `Set` tracks keys for questions already in the current deck. Format: `` `${type}|${flagId}|${scope}` ``.

| Type | `flagId` | `scopeVersion` | Example key |
|------|----------|----------------|-------------|
| `versionPresence` | Flag under test | Always **25** | `versionPresence|UseG1GC|25` |
| `description` | Correct flag | Always **25** | `description|UseG1GC|25` |
| `flagFromDescription` | Correct flag | Always **25** | `flagFromDescription|UseG1GC|25` |
| `flagCount` | `_catalog` (synthetic) | Always **25** | `flagCount|_catalog|25` |

The same `flagId` may appear in **different** question types in one game. It cannot appear twice with the same type and scope.

---

## Type 1: Version check (`versionPresence`)

**UI label:** Version check  
**Prompt:** “Is this flag available in **Java 25**?”

| Rule | Detail |
|------|--------|
| Target version | Always **Java 25** |
| Yes/No balance | 50% “present”, 50% “absent” |
| If **present** | Pick a random flag from the **Java 25** catalog |
| If **absent** | Pick a random flag whose `flagId` is **not** in Java 25 (from any loaded catalog) |
| Options | `["Yes", "No"]` shuffled |
| Abort | `null` if Java 25 has no flags, or no absent flags exist |

This type covers LTS availability for the quiz scope. There is no separate “pick an LTS that includes this flag” question — that overlapped with version check and was weak for Java 25 learners (Java 25 was often one of several correct answers).

---

## Type 2: Flag description (`description`)

**UI label:** Flag description  
**Prompt:** “Which description matches this JVM flag?”

| Rule | Detail |
|------|--------|
| Scope version | Always **Java 25** |
| Sibling pool | Parent group must have **≥ 4 flags** in the Java 25 catalog **and** **≥ 4 distinct** `description` values among those flags |
| Correct flag | Random flag from that group |
| Distractors | 3 other flags from the **same parent group** (siblings) |
| Options | Four descriptions (1 correct + 3 distractors), shuffled |
| Unique options | All four option texts must be **distinct**; discard if any duplicate |
| Display | Shows the **correct** flag’s `flag` string (Java 25 wording) |
| Abort | `null` if no eligible group, or if the four chosen descriptions are not all unique |

---

## Type 3: Flag name (`flagFromDescription`)

**UI label:** Flag name  
**Prompt:** “Which JVM flag matches this description?”

| Rule | Detail |
|------|--------|
| Scope version | Always **Java 25** |
| Sibling pool | Same eligibility as **Type 2** (`pickEligibleSiblingGroup` on Java 25) |
| Correct flag | Random flag from that group |
| Distractors | 3 sibling `flag` strings from Java 25 |
| Options | Four flag names, shuffled |
| Unique options | All four `flag` strings must be **distinct** |
| Display | Shows the **correct** flag’s `description` in the monospace block |
| Abort | `null` if no eligible group, or if the four chosen flag names are not all unique |

---

## Type 4: Catalog size (`flagCount`)

**UI label:** Catalog size  
**Prompt:** “How many JVM flags are cataloged for **Java 25** on this site?”

| Rule | Detail |
|------|--------|
| Target version | Always **Java 25** (correct count) |
| Correct answer | Count of flag nodes with a non-empty `description` in the Java 25 catalog |
| Wrong answers | Flag counts from **three other** LTS catalogs (8, 11, 17, 21) |
| Options | Four numbers as strings, shuffled |
| Display | No flag monospace block (prompt only) |
| Abort | `null` if Java 25 has no flags or fewer than three distinct distractor counts |

---

## Presentation rules (all types)

- **4 multiple-choice options** per question (binary for version presence; four descriptions, flag names, or numbers for the other types).
- **Monospace block** — Flag string for version check and description questions; description text for `flagFromDescription`; omitted for `flagCount`.
- Options escaped with `escapeHtml`.
- **Shuffle** uses Fisher–Yates on arrays; **pickRandom** is uniform.

## Scoring and replay

- Pass when `score >= WIN_THRESHOLD`, where `WIN_THRESHOLD = ceil(TOTAL_QUESTIONS × 70 / 100)` (14 for 20 questions).
- One click per question; correct if `selectedIndex === q.correctIndex`.
- **Play again** rebuilds a new random set with the same index (same loaded JSON).
- If fewer than 20 questions after generation, show: “Could not generate enough questions.”
- On failure, the result screen lists incorrect answers with your choice vs. the correct option.

## Practical implications

- Learners see **Java 25** flag names and descriptions for knowledge questions; older-only flags appear in “not available in Java 25” version checks.
- **Description / flag name** questions only use sibling groups from the Java 25 taxonomy (≥ 4 flags, ≥ 4 distinct descriptions in the group).
- **Version presence** is the only cross-LTS question type; it always asks about Java 25.
- **Catalog size** always asks for the Java 25 count; distractors are the other LTS catalog sizes.
- **Balanced mix** — Every game includes exactly one catalog-size question; the other three types share the remaining 19 slots (typically 7 + 7 + 5).

## Source references

| Constant / function | Location in `docs/quiz-game.html` |
|---------------------|-----------------------------------|
| `LTS_VERSIONS`, `QUIZ_SCOPE_VERSION`, `TOTAL_QUESTIONS`, `PASS_PERCENT`, `WIN_THRESHOLD` | ~464–468 |
| `TYPE_LABELS` | ~470–475 |
| `parseGraph`, `buildFlagIndex` | ~489–555 |
| `flagInVersion`, `pickFlagFromVersion`, `buildVersionPresenceQuestion` | ~557–594 |
| `pickEligibleSiblingGroup`, `buildDescriptionQuestion` | ~596–633 |
| `buildFlagFromDescriptionQuestion` | ~635–660 |
| `buildFlagCountQuestion` | ~662–687 |
| `buildBalancedQuizPlan`, `questionDedupKey`, `generateQuiz` | ~702–747 |
