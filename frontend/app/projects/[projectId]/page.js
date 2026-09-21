"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { getProject } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";
import MaintainersPanel from "../../components/MaintainersPanel";

export default function ProjectDetail() {
  const params = useParams();
  const projectId = params?.projectId;
  const { user: currentUser } = useAuth();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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

  const handleMaintainerAdded = (newMaintainer) => {
    // Refresh the project to get updated maintainers list
    if (projectId && typeof projectId === "string") {
      getProject(projectId).then(setProject).catch(console.error);
    }
  };

  const handleMaintainerRemoved = (userId) => {
    // Update local state by filtering out the removed maintainer
    setProject((prev) => ({
      ...prev,
      maintainers: prev.maintainers.filter((m) => m.user?.id !== userId),
    }));
  };

  if (loading) {
    return <p>Loading project...</p>;
  }

  if (error) {
    return <p role="alert">{error}</p>;
  }

  if (!project) {
    return <p>Project not found.</p>;
  }

  return (
    <main>
      <Link href="/projects">← Back to projects</Link>

      <header>
        <h1>{project.name}</h1>

        {project.description && <p>{project.description}</p>}

        <div>
          {project.primaryLanguage && (
            <span>{project.primaryLanguage}</span>
          )}

          {project.category && (
            <span> · {project.category}</span>
          )}

          {project.connection && (
            <span> · {project.connection}</span>
          )}
        </div>
      </header>

      <section>
        <h2>Project information</h2>
        <p>Owner: {project.owner || "Unknown"}</p>
        <p>License: {project.license || "Unknown"}</p>
        <p>Stars: {project.stars ?? 0}</p>
        <p>Forks: {project.forks ?? 0}</p>
        <p>Open issues: {project.openIssues ?? 0}</p>
        <p>Contributors: {project.contributors ?? 0}</p>

        {project.githubUrl && (
          <p>
            <a
              href={project.githubUrl}
              target="_blank"
              rel="noreferrer"
            >
              GitHub repository
            </a>
          </p>
        )}
      </section>

      <section>
        <h2>Maintainers</h2>

        {project.maintainers?.length > 0 ? (
          <ul>
            {project.maintainers.map((maintainer, index) => (
              <li key={maintainer.user?.id ?? index}>
                {maintainer.user?.username ??
                  maintainer.user?.login ??
                  maintainer.user?.name ??
                  String(maintainer)}
              </li>
            ))}
          </ul>
        ) : (
          <p>No maintainers listed.</p>
        )}
      </section>

      {/* Maintainers panel — only shows for maintainers */}
      <MaintainersPanel
        project={project}
        isMaintainer={isMaintainer}
        onMaintainerAdded={handleMaintainerAdded}
        onMaintainerRemoved={handleMaintainerRemoved}
      />

      <section>
        <h2>Featured issues</h2>

        {project.featuredIssues?.length > 0 ? (
          <ul>
            {project.featuredIssues.map((issue, index) => (
              <li key={issue.id ?? index}>
                <Link href={`/issues/${issue.id}`}>
                  {issue.title ?? `Issue #${issue.id}`}
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <p>No featured issues.</p>
        )}
      </section>

      <section>
        <h2>Recent comments</h2>

        {project.recentComments?.length > 0 ? (
          <ul>
            {project.recentComments.map((comment, index) => (
              <li key={comment.id ?? index}>
                {comment.body ??
                  comment.content ??
                  String(comment)}
              </li>
            ))}
          </ul>
        ) : (
          <p>No recent comments.</p>
        )}
      </section>
    </main>
  );
}