'use client';

import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function AuthButtons() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return <div className="text-sm text-gray-500">Loading auth...</div>;
  }

  if (!user) {
    return (
      
        href="/auth/github"
        className="inline-flex items-center gap-2 px-4 py-2 bg-gray-900 text-white rounded-full text-sm font-semibold hover:bg-gray-800 transition-all"
      >
        Log in with GitHub
      </a>
    );
  }

  return (
    <div className="flex items-center gap-4">
      {user.avatar && (
        <img
          src={user.avatar}
          alt={user.name}
          className="w-8 h-8 rounded-full"
        />
      )}
      <span className="text-sm font-medium">{user.name}</span>
      <button
        onClick={logout}
        className="px-3 py-1 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded transition-all"
      >
        Log out
      </button>
    </div>
  );
}