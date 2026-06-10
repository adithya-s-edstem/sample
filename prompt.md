- Todo number is of the format PN-n. Example: P1-1, P2-4 etc.
- There are exit checks at the end of each phase in the todo.
- Subagents must run under their own context.

Take each item in todo.md and dispatch a subagent with it's own context with the following subagent-prompt passing the todo number. If the item is last in a phase, also provide the exit check to the agent.

<subagent-prompt>
Create a todo from tasks. Implement each items from the task, mark the item as either done|failed|not_provided.
Use env for any environment variables you need.

<env>
JAVA_HOME="C:\Users\adithya\tools\jdk-21.0.11+10"
</env>

<tasks>
<task>
implementation: Use the repo skill /implement-todo for the todo number.
</task>
<task>
exit_check: If an exit check is provided, investigate, mark it as complete if the checks pass. And add it to the open PR.
</task>
<task>
pr: Once the PR is raised follow pr-steps.
</task>
</tasks>

<pr-steps>
1. Perform a review of the PR against the spec.
2. Post your review comment to the PR.
3. Fix the review comment if any issues are found.
4. Loop 1-3 until the PR matches the spec.
5. Merge the PR.
</pr-steps>

<return>
Once the task is completed, the agent should only return the following json
```json
{
  implementation: "done|failed",
  exit_check: "done|failed|not_provided",
  pr: "done|failed"
}
```
</return>
</subagent-prompt>

Only take the next item after the agent returns {implementation: "done", exit_check: "done|not_provided", pr: "done"}. If it returns either implementation, pr or exit_check as failed, stop.
