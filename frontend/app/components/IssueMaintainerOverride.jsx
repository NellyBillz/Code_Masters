"use client";

import { useEffect, useState } from "react";
import { getProject } from "../../lib/api";
import { useAuth } from "../context/AuthContext";
import DifficultyOverride from "./DifficultyOverride";

export default function IssueMaintainerOverride({ issue }) {
  const { user: currentUser } = useAuth();
  const [project, setProject] = useState(null);

  useEffect(() => {
    if (!issue?.project?.id) return;

    getProject(issue.project.id)
      .then(setProject)
      .catch(() => setProject(null));
  }, [issue?.project?.id]);

  const isMaintainer = project?.maintainers?.some(
    (maintainer) =>
      maintainer.user?.id === currentUser?.id ||
      maintainer.user?.username === currentUser?.username
  );

  return (
    <DifficultyOverride
      issueId={issue.id}
      currentDifficulty={issue.difficulty}
      currentIsBeginnerFriendly={issue.isBeginnerFriendly}
      isMaintainer={isMaintainer}
    />
  );
}
