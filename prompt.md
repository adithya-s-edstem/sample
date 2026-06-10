- Todo number is of the format PN-n. Example: P1-1, P2-4 etc.
- There are exit checks at the end of each phase in the todo, verify the checks and mark them as done in the PR for the very last item in that phase.
- Subagents must run under their own context.

Take each item in todo.md and dispatch a subagent with it's own context with the following subagent-prompt passing the todo number. If the item is last in a phase, also provide the exit check to the agent.

<subagent-prompt>
Create a todo from tasks. Implement each items from the task, mark the item as done.

<env>
JAVA_HOME="C:\Users\adithya\tools\jdk-21.0.11+10"
</env>

<tasks>
<task>
implementation: /implement-todo for the todo number following the rules.
</task>
<task>
exit_check: If an exit check is provided, investigate, mark it as complete if the checks pass.
</task>
<task>
Once the PR is raised follow pr-steps.
</task>
</tasks>

<pr-steps>
1. Perform a review of the PR against the spec.
2. Post your review comment to the PR.
3. Fix the review comment if any issues are found.
4. Loop 1-3 until the PR matches the spec.
5. Merge the PR.
</pr-steps>

<rules>
<rule>
don't ask me for anything. 
</rule>
</rules>

<return>
Once the task is completed, the agent should return the following json
<code>
{implementation: "done|failed", exit_check: "done|failed"}
</code>
</return>
</subagent-prompt>

Only take the next item after the agent returns {implementation: "done", exit_check: "done"}. If it returns either implementation or exit_check as failed, stop.
