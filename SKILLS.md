# SKILLS.md

Reference for how each tool in the workflow should be used on this project.
Not all agents read this automatically — paste relevant sections in when
starting a new Aider/Cline session if it doesn't pick up context.

## Aider — implementation agent
- Always scope files explicitly with `/add` before instructing; don't let
  it roam the whole repo per step.
- Work in the slice order from AGENTS.md: migration → entity → repository
  → service+test → controller+test → wiring.
- After each Aider-generated step: run `mvn test` before moving to the next
  step. If it fails, fix in the same step — never carry a broken build
  forward.
- Use `/diff` to review before accepting if a change looks larger than
  expected for the step.
- Switch to diff edit format once files exceed ~150 lines (cheaper, faster,
  less risk of accidental unrelated rewrites).

## Cline (IntelliJ/VS Code chat) — explain & refine
- Use for: "explain this method", "why would this query N+1", "tighten
  this validation logic" — single-file, single-concept questions.
- Not for: multi-file feature generation — that's Aider's job. Keep the
  division clean so context doesn't get duplicated across tools.

## Claude chat — design & documentation
- Owns `docs/DESIGN.md` — schema, endpoint contracts, auth rules, edge
  cases. Written and refined here before any code exists.
- Used for translating an interview-style verbal spec into a structured
  doc Aider can execute against literally.
- Also used for: README pass, CHANGELOG summary pass at the end of a
  sprint (condense raw commit messages into a readable log).

## Windsurf — autocomplete only
- Inline completion while hand-editing a file Aider already created.
- Not used for agentic/multi-file changes — avoid overlapping responsibility
  with Aider to prevent conflicting edits.

## Handoff rule
Design doc (Claude chat) → implementation plan (broken into Aider-sized
steps) → Aider executes one step → Cline/Windsurf assist on the specific
file being reviewed → next step. Never skip the design doc step, even
under time pressure — undocumented scope is how agents drift.