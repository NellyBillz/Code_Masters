"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { getProject, updateProject, reportProject } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";
import MaintainersPanel from "../../components/MaintainersPanel";
import ReportButton from "../../components/ReportButton";

export default function ProjectDetail() {
  const params = useParams();
  const projectId = params?.projectId;
  const { user: currentUser } = useAuth();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [togglingIntake, setTogglingIntake] = useState(false);
  const [intakeError, setIntakeError] = useState("");

  const isMaintainer = Boolean(
    currentUser &&
      project?.maintainers?.some(
        (maint) =>
          maint.user?.id === currentUser.id ||
          maint.user?.username === currentUser.username
      )
  );

  useEffect(() => {
    if (!projectId) return;

    let cancelled = false;

    async function loadProject() {
      setLoading(true);
      setError("");

      try {
        const projectResult = await getProject(projectId);

        if (!cancelled) {
          setProject(projectResult);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || "Failed to load project.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadProject();

    return () => {
      cancelled = true;
    };
  }, [projectId]);

  const handleMaintainerAdded = () => {
    if (projectId && typeof projectId === "string") {
      getProject(projectId).then(setProject).catch(console.error);
    }
  };

  const handleMaintainerRemoved = (userId) => {
    setProject((prev) => ({
      ...prev,
      maintainers: prev.maintainers.filter((m) => m.user?.id !== userId),
    }));
  };

  const handleToggleIntake = async () => {
    if (!project) return;

    setTogglingIntake(true);
    setIntakeError("");

    try {
      const updated = await updateProject(projectId, {
        acceptingContributions: !project.acceptingContributions,
      });
      setProject((prev) => ({
        ...prev,
        acceptingContributions: updated.acceptingContributions,
      }));
    } catch (err) {
      setIntakeError(err.message || "Failed to update contribution intake.");
    } finally {
      setTogglingIntake(false);
    }
  };

  if (loading) {
    return (
      <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
          <p className="text-sm font-medium text-slate-600">
            Loading project...
          </p>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div
          role="alert"
          className="rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-700"
        >
          {error}
        </div>
      </main>
    );
  }

  if (!project) {
    return (
      <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
          <h1 className="text-xl font-bold text-slate-900">
            Project not found
          </h1>
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8">
        <Link
          href="/projects"
          className="inline-flex items-center text-sm font-medium text-blue-600 transition hover:text-blue-800"
        >
          ← Back to projects
        </Link>
      </div>

      {project.listingStatus === "pending" && (
        <div
          role="status"
          className="mb-8 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800"
        >
          <p className="font-semibold">Pending review</p>
          <p className="mt-1">
            This project isn&apos;t public yet. A site admin needs to
            approve it before it appears in search and the projects list —
            only you and its other maintainers can see this page in the
            meantime.
          </p>
        </div>
      )}

      {project.listingStatus === "rejected" && (
        <div
          role="status"
          className="mb-8 rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-800"
        >
          <p className="font-semibold">Submission rejected</p>
          <p className="mt-1">
            A site admin reviewed this submission and it wasn&apos;t
            approved. It stays hidden from public discovery — only you and
            its other maintainers can see this page.
          </p>
        </div>
      )}

      <header className="mb-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="mb-4 flex flex-wrap gap-2">
          {project.primaryLanguage && (
            <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
              {project.primaryLanguage}
            </span>
          )}

          {project.category && (
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
              {project.category}
            </span>
          )}

          {project.connection && (
            <span className="rounded-full bg-green-50 px-3 py-1 text-xs font-semibold text-green-700">
              {project.connection}
            </span>
          )}

          {project.hasContributingGuide && (
            <span className="rounded-full bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">
              Has contributing guide
            </span>
          )}
        </div>

        <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
          {project.name}
        </h1>

        {project.description && (
          <p className="mt-4 max-w-3xl text-base leading-7 text-slate-600">
            {project.description}
          </p>
        )}

        {project.githubUrl && (
          <div className="mt-6">
            <a
              href={project.githubUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex rounded-xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
            >
              View GitHub repository
            </a>
          </div>
        )}

        <div className="mt-6 flex flex-wrap items-center gap-4 border-t border-slate-100 pt-6">
          <span
            className={`rounded-full px-3 py-1 text-xs font-semibold ${
              project.acceptingContributions
                ? "bg-green-50 text-green-700"
                : "bg-slate-100 text-slate-600"
            }`}
          >
            {project.acceptingContributions
              ? "Accepting contributions"
              : "Not currently accepting contributions"}
          </span>

          {isMaintainer && (
            <button
              type="button"
              onClick={handleToggleIntake}
              disabled={togglingIntake}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {togglingIntake
                ? "Saving..."
                : project.acceptingContributions
                  ? "Pause new contributions"
                  : "Resume accepting contributions"}
            </button>
          )}

          {currentUser && (
            <ReportButton
              label="Report this project"
              onSubmit={(reason) => reportProject(projectId, reason)}
            />
          )}
        </div>

        {intakeError && (
          <p role="alert" className="mt-2 text-sm text-red-700">
            {intakeError}
          </p>
        )}
      </header>

      <section className="mb-8">
        <h2 className="mb-4 text-xl font-bold text-slate-900">
          Project information
        </h2>

        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
              Owner
            </p>
            <p className="mt-2 font-semibold text-slate-900">
              {project.owner || "Unknown"}
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
              License
            </p>
            <p className="mt-2 font-semibold text-slate-900">
              {project.license || "Unknown"}
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 text-center shadow-sm">
            <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
              Stars
            </p>
            <p className="mt-2 text-2xl font-bold text-slate-900">
              {project.stars ?? 0}
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 text-center shadow-sm">
            <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
              Forks
            </p>
            <p className="mt-2 text-2xl font-bold text-slate-900">
              {project.forks ?? 0}
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 text-center shadow-sm">
            <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
              Open issues
            </p>
            <p className="mt-2 text-2xl font-bold text-slate-900">
              {project.openIssues ?? 0}
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 text-center shadow-sm">
            <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
              Contributors
            </p>
            <p className="mt-2 text-2xl font-bold text-slate-900">
              {project.contributors ?? 0}
            </p>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-xl font-bold text-slate-900">
            Maintainers
          </h2>

          {project.maintainers?.length > 0 ? (
            <ul className="space-y-3">
              {project.maintainers.map((maintainer, index) => (
                <li
                  key={maintainer.user?.id ?? index}
                  className="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3"
                >
                  <span className="font-medium text-slate-800">
                    {maintainer.user?.username ??
                      maintainer.user?.login ??
                      maintainer.user?.name ??
                      String(maintainer)}
                  </span>

                  {maintainer.role && (
                    <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold capitalize text-blue-700">
                      {maintainer.role}
                    </span>
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-500">
              No maintainers listed.
            </p>
          )}
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-xl font-bold text-slate-900">
            Featured issues
          </h2>

          {project.featuredIssues?.length > 0 ? (
            <ul className="space-y-3">
              {project.featuredIssues.map((issue, index) => (
                <li key={issue.id ?? index}>
                  <Link
                    href={`/issues/${issue.id}`}
                    className="block rounded-xl border border-slate-200 p-4 transition hover:border-blue-300 hover:bg-blue-50"
                  >
                    <span className="font-medium text-slate-900">
                      {issue.title ?? `Issue #${issue.id}`}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-500">
              No featured issues.
            </p>
          )}
        </section>
      </div>

      <MaintainersPanel
        project={project}
        isMaintainer={isMaintainer}
        onMaintainerAdded={handleMaintainerAdded}
        onMaintainerRemoved={handleMaintainerRemoved}
      />

      <section className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-xl font-bold text-slate-900">
          Recent comments
        </h2>

        {project.recentComments?.length > 0 ? (
          <ul className="space-y-3">
            {project.recentComments.map((comment, index) => (
              <li
                key={comment.id ?? index}
                className="rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-700"
              >
                {comment.body ??
                  comment.content ??
                  String(comment)}
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-slate-500">
            No recent comments.
          </p>
        )}
      </section>
    </main>
  );
}
