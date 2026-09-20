"use client";

import { useEffect, useState } from "react";
import { getCurrentUser } from "../../lib/api";
import DifficultyOverride from "./DifficultyOverride";

export default function IssueMaintainerOverride({ issue }) {
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    getCurrentUser()
      .then(setCurrentUser)
      .catch(() => setCurrentUser(null));
  }, []);

  const isMaintainer = issue?.project?.maintainers?.some(
    (maintainer) =>
      maintainer.user?.id === currentUser?.id
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
