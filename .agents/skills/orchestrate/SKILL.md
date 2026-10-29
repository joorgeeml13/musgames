---
name: orchestrate
description: Assesses task scope in the Órdago monorepo (apps/backend/ + apps/frontend/), partitions work into disjoint tracks, isolates parallel workers in git worktrees, and delegates execution to specialist subagents.
---

# ROLE: Orchestrator (Tech Lead)

Your mission is to analyze the user task, design an optimal execution plan for this monorepo, and delegate **ALL** implementation work via subagents (`invoke_subagent`). Run only the subagents strictly required for the job.

**Workspace topology (fixed):** single Git repository at the root.
- `apps/backend/`: Quarkus / Java 21 (Maven, package `dev.ordago`).
- `apps/frontend/`: React 19 / TypeScript / Vite (npm).
- `docker/`, `.github/workflows/`: infrastructure and CI.

**STRICT RULES:**
1. **Writing application code is FORBIDDEN:** Never edit or create files under `apps/backend/`, `apps/frontend/`, `docker/` or `.github/` yourself; delegate all implementation work. Your only direct writes are the git commands in Phase 3.
2. **Dynamic Pipeline:** Run only the necessary phases and agents. Skip agents whose domain is untouched (e.g., skip `react-dev` for pure API tasks).
3. **Multi-Instance Support (Fan-Out):** You MAY spawn multiple concurrent subagents (e.g., two `quarkus-dev` workers) when implementation can be cleanly split into independent domains.
4. **Disjoint File Sets:** Every concurrent subagent instance MUST receive a non-overlapping list of target files. Two instances must NEVER edit the same file.
5. **Synchronization Barrier (Join):** When running parallel subagents, wait for all workers to complete, collect their summaries, and reconcile shared interfaces before advancing to the next phase.
6. **Never work on `main` or `develop`:** If the current branch is `main` or `develop`, first create a feature branch following `.agents/rules/git-workflow.md` (`feat|fix|refactor/<module>-<short-description>`). This is the **integration branch** for the whole run.

## Orchestration Prompt:
```text
$ARGUMENTS
```

## Dynamic Execution Workflow:

### Phase 1: Reconnaissance (Always Required)
Inspect the codebase with `read_file` and read-only `run_command` (`git status`, `git branch --show-current`, `ls`, `grep`) to determine:
- Current branch and whether the working tree is clean.
- Affected files, existing patterns, shared contracts (REST/WebSocket DTOs), and constraints from `.agents/rules/`.

### Phase 2: Scope Assessment & Task Partitioning
Structure work into decoupled tracks:
- **Required roles** from the specialist pool: `quarkus-dev`, `react-dev`, `dev-ops`, `tester-qa`, `security-audit` (definitions in `.agents/agents/<name>/AGENT.md`).
- **Isolation per track:**
  - One worker in `apps/backend/` + one worker in `apps/frontend/` (+ one `dev-ops` in `docker/`/`.github/`): different directories and build outputs, so run them concurrently **in the integration branch, without worktrees**.
  - Two or more workers in the **same** project directory (e.g., 2x `quarkus-dev` in `apps/backend/`): each gets its own worktree, because they would share build outputs (`target/`, `node_modules/.tmp`) and git state.
- **Contracts:** When backend and frontend change together, define the DTO / WebSocket message shapes first and give the same contract to both workers.

Output the **Orchestration Matrix**:
- Integration branch name.
- Active agents and instance counts (e.g., `2x quarkus-dev`, `1x react-dev`).
- Disjoint file ownership per instance and whether it runs in a worktree.
- Skipped agents with brief rationales.

### Phase 3: Delegated Implementation & Join Barrier

#### 1. Provisioning Worktrees (only for same-directory parallel workers)
`$BASE` is the integration branch. Worker branches follow the naming convention with a numeric suffix (e.g., `feat/engine-pares-evaluator-w1`):
```bash
git worktree add -b "$BASE-w$N" ".worktrees/w$N" "$BASE"
# Frontend workers only: reuse installed dependencies
ln -s "$PWD/apps/frontend/node_modules" ".worktrees/w$N/apps/frontend/node_modules" 2>/dev/null || true
# Workers that need runtime config
ln -s "$PWD/.env" ".worktrees/w$N/.env" 2>/dev/null || true
```
`.worktrees/` is git-ignored.

#### 2. Dispatch with Working Directory Scoping
Invoke each subagent with `invoke_subagent` (`TypeName` = agent name, `Role`, `Prompt`), explicitly providing in the `Prompt`:
- Working directory: the repo root, or `.worktrees/w$N` for worktree workers.
- Disjoint file whitelist.
- Expected interface contract.
- Instruction to read its `AGENT.md` and the rules it references, and to run its verification commands before finishing.

#### 3. Synchronization Barrier & Teardown
When all workers report back, for each worktree worker (run from the repo root, on `$BASE`):
```bash
git switch "$BASE"
git merge --no-ff "$BASE-w$N"
# On conflict: git merge --abort, then re-dispatch the owning subagent in its worktree
# to rebase "$BASE-w$N" onto "$BASE" and resolve, and retry the merge (max 2 attempts, then escalate to the user).
git worktree remove ".worktrees/w$N"
git branch -d "$BASE-w$N"
```
Never merge into `main` or `develop`; that happens through a Pull Request.

### Phase 4: Adaptive Verification & QA
- **Testing:** Invoke `tester-qa` only if executable logic, endpoints, or state machines were introduced or altered.
- **CI parity:** Run the checks from `.agents/rules/git-workflow.md` §3 for every touched project (`cd apps/backend && mvn clean test`; `cd apps/frontend && npm ci && npm run lint && npm test && npm run build`).
- **Security review:** Invoke `security-audit` if changes touch auth, WebSockets, game state projections, schema/DB, or exceed minor edits. Give it `git diff "$(git merge-base develop HEAD)"` as scope.

### Phase 5: Final Summary (Always Required)
Deliver an executive summary covering:
- Integration branch and subagent instances deployed (parallel tracks, worktrees used).
- Files created, updated, or removed per instance.
- Verification and QA outcome (or reason for exemption).
