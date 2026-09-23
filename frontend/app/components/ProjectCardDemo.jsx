import ProjectCard from "./ProjectCard";
import { mockProjects } from "../data/mockProjects";

/**
/**
 * Visual QA harness for FE-01.7. Not a page the app routes to, just a
 * grid so the card's every badge state can be reviewed side by side:
 *   - verified + beginner-friendly       (httpie)
 *   - verified + not beginner-friendly   (tokio)
 *   - unverified + beginner-friendly     (first-timers-cookbook)
 *   - unverified + not beginner-friendly (legacy-etl-pipeline)
 *   - missing-field fallback             (quiet-utils)
 */
export default function ProjectCardDemo() {
  return (
    <div className="min-h-screen bg-slate-50 p-8">
      <h1 className="mb-1 text-xl font-semibold text-slate-900">
        ProjectCard, badge state review
      </h1>
      <p className="mb-6 text-sm text-slate-500">
        5 mock projects covering every verified / hasBeginnerFriendlyIssues
        combination, plus one missing-field edge case.
      </p>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {mockProjects.map((project) => (
          <ProjectCard key={project.name} project={project} />
        ))}
      </div>
    </div>
  );
}
