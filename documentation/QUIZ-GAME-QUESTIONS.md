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
**Prompt:** “Which **LTS release** includes this flag?”

| Rule | Detail |
|------|--------|
| Flag pool | Only `flagId`s that appear in **at least one** LTS version |
| Correct answer | Random **one** LTS version where that `flagId` exists (not necessarily the only one) |
| Wrong answers | Up to 3 LTS versions where the flag is **not** present |
| Fallback for wrong options | If fewer than 3 absent LTS versions, fill from other LTS versions (excluding correct and already chosen wrong) |
| Options | Four labels: `Java {n}` — correct + 3 wrong, all shuffled |
| Abort | `null` if no eligible `flagId` |

---

## Type 3: Flag description (`description`)

**UI label:** Flag description  
**Prompt:** “Which description matches this JVM flag?”

| Rule | Detail |
|------|--------|
| Scope version | Random LTS |
| Sibling pool | Parent group must have **≥ 4 flags** in that version |
| Correct flag | Random flag from that group |
| Distractors | 3 other flags from the **same parent group** (siblings) |
| Options | Four descriptions (1 correct + 3 distractors), shuffled |
| Display | Shows the **correct** flag’s `flag` string |
| Abort | `null` if no group with ≥ 4 members in that version |

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

- **Description questions** only use flags that share a category with at least three other flags in that JDK.
- **Which-version** can mark an LTS as correct even if the flag also exists in other LTS releases (any qualifying LTS is valid).
- **Version presence** “absent” uses a flag from *some* catalog that is missing in the asked version—not a synthetic flag name.

## Source references

| Constant / function | Location in `docs/quiz-game.html` |
|---------------------|-----------------------------------|
| `LTS_VERSIONS`, `TOTAL_QUESTIONS`, `WIN_THRESHOLD` | ~463–465 |
| `parseGraph`, `buildFlagIndex` | ~486–551 |
| `buildVersionPresenceQuestion` | ~553–581 |
| `buildWhichVersionQuestion` | ~583–616 |
| `buildDescriptionQuestion` | ~618–640 |
| `generateQuiz` | ~642–657 |
