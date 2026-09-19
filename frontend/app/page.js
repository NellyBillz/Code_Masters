'use client';

import React, { useEffect, useState } from 'react';
import { ChevronRight } from 'lucide-react';
import { listProjects } from '../lib/api';
import ProjectCard from './components/ProjectCard';

const FEATURED_SIZE = 6;
const MORE_SIZE = 6;

export default function CodeMastersHome() {
  const [featured, setFeatured] = useState([]);
  const [more, setMore] = useState([]);
  const [total, setTotal] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError('');
      try {
        const [top, next] = await Promise.all([
          listProjects({ sort: 'stars', size: FEATURED_SIZE, page: 0 }),
          listProjects({ sort: 'stars', size: MORE_SIZE, page: 1 }),
        ]);
        if (cancelled) return;
        setFeatured(top.items);
        setMore(next.items);
        setTotal(top.meta.total);
      } catch (err) {
        if (cancelled) return;
        setFeatured([]);
        setMore([]);
        setTotal(null);
        setError(err.message || 'Failed to load projects.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, []);

  const remaining = total === null ? 0 : Math.max(total - featured.length, 0);

  return (
    <div className="min-h-screen bg-gray-100">
      <nav className="fixed top-4 left-1/2 transform -translate-x-1/2 z-50 bg-gray-900 text-white rounded-full px-6 py-3 flex items-center gap-6 shadow-lg max-w-fit">
        <div className="font-bold text-lg tracking-tight">Code Masters</div>
        <div className="bg-green-500 text-white px-4 py-2 rounded-full text-sm font-semibold flex items-center gap-2">
          <div className="w-4 h-4 bg-white/30 rounded-full"></div>
          Discover & Explore
        </div>
        <a href="/projects" className="bg-white text-gray-900 px-4 py-2 rounded-full text-sm font-semibold hover:bg-green-500 hover:text-white transition-all">
          Browse Projects
        </a>
      </nav>
      <div className="fixed left-6 top-1/2 transform -translate-y-1/2 w-10 h-10 bg-white border border-gray-300 rounded-full flex items-center justify-center cursor-pointer z-40 hover:shadow-md transition-all">←</div>
      <div className="max-w-6xl mx-auto p-6 pt-32">
        <section className="bg-white rounded-lg p-12 mb-12 border border-gray-200">
          <h1 className="text-5xl font-bold leading-tight mb-4">The front door to African <span className="text-green-500">open-source</span> discovery</h1>
          <p className="text-lg text-gray-600 mb-6 leading-relaxed max-w-2xl">Explore projects, connect with developers, and discover the innovation happening across Africa's open-source communities.</p>
          <a href="/projects" className="inline-flex items-center gap-2 bg-green-500 text-white font-semibold px-6 py-3 rounded hover:bg-green-600 transition-all">
            Explore Projects
            <ChevronRight size={18} />
          </a>
        </section>
        <div className="flex items-center justify-between mb-8 flex-wrap gap-4">
          <h2 className="text-3xl font-bold">Projects</h2>
          {total !== null && (
            <div className="text-center">
              <div className="text-3xl font-bold text-green-500">{total}</div>
              <div className="text-xs text-gray-600 font-semibold uppercase tracking-wider">{total === 1 ? 'Project' : 'Projects'}</div>
            </div>
          )}
        </div>
        {loading && <p className="text-gray-600 mb-8">Loading projects...</p>}
        {error && <p role="alert" className="text-red-600 mb-8">{error}</p>}
        {!loading && !error && featured.length === 0 && <p className="text-gray-600 mb-8">No projects found yet.</p>}
        {!loading && !error && featured.length > 0 && (
          <section className="mb-12">
            <div className="mb-6">
              <h3 className="text-lg font-bold">Featured Projects <span className="text-gray-600 font-normal text-sm">top {featured.length} by stars</span></h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {featured.map((project) => (
                <ProjectCard key={project.id} project={project} />
              ))}
            </div>
          </section>
        )}
        {!loading && !error && more.length > 0 && (
          <section>
            <div className="flex items-center justify-between mb-6 flex-wrap gap-4">
              <h3 className="text-lg font-bold">Explore More <span className="text-gray-600 font-normal text-sm">{remaining} more {remaining === 1 ? 'project' : 'projects'}</span></h3>
              <a href="/projects" className="px-4 py-2 bg-white text-gray-700 border border-gray-300 rounded-full text-sm font-medium hover:bg-green-500 hover:text-white transition-all">View All →</a>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {more.map((project) => (
                <ProjectCard key={project.id} project={project} />
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
