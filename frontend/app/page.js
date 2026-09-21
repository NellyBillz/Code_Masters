'use client';

import React, { useState } from 'react';
import { ChevronRight } from 'lucide-react';

export default function CodeMastersHome() {
  const [activeFilter, setActiveFilter] = useState('all');

  const projects = [
    {
      id: 1,
      title: 'Code Masters API',
      subtitle: 'Platform Foundation',
      description: 'RESTful API powering South African open-source discovery. Built with Node.js and PostgreSQL.',
      avatar: 'CM',
      tags: ['Node.js', 'API', 'Open Source'],
      stars: '2.3K',
      updated: 'Updated today',
      featured: true,
    },
    {
      id: 2,
      title: 'SA Vision',
      subtitle: 'Computer Vision Library',
      description: 'Open-source ML toolkit for South African-specific computer vision applications.',
      avatar: 'SV',
      tags: ['Python', 'ML', 'CV'],
      stars: '1.8K',
      updated: '2 days ago',
    },
    {
      id: 3,
      title: 'Data Toolkit',
      subtitle: 'Analytics Framework',
      description: 'Lightweight data processing and analytics framework for development teams.',
      avatar: 'DT',
      tags: ['Python', 'Data', 'Analytics'],
      stars: '1.2K',
      updated: '1 week ago',
    },
    {
      id: 4,
      title: 'Weave Design',
      subtitle: 'UI Component Library',
      description: 'Modern React components designed for South African tech products and applications.',
      avatar: 'WD',
      tags: ['React', 'UI', 'Design System'],
      stars: '956',
      updated: '3 days ago',
    },
    {
      id: 5,
      title: 'LangServe SA',
      subtitle: 'Language Processing',
      description: 'NLP tools optimized for South African languages including Zulu, Xhosa, and Afrikaans.',
      avatar: 'LS',
      tags: ['NLP', 'Languages', 'ML'],
      stars: '745',
      updated: '1 week ago',
    },
  ];

  const moreProjects = [
    {
      id: 6,
      title: 'MobileBase',
      subtitle: 'Mobile Backend',
      description: 'Backend-as-a-service platform for mobile apps across South Africa.',
      avatar: 'MB',
      tags: ['Backend', 'Mobile'],
      stars: '523',
      updated: '2 weeks ago',
    },
    {
      id: 7,
      title: 'FinConnect',
      subtitle: 'Fintech Library',
      description: 'Open-source fintech integration library for South African payment systems.',
      avatar: 'FC',
      tags: ['Fintech', 'Payments'],
      stars: '412',
      updated: '3 weeks ago',
    },
    {
      id: 8,
      title: 'EduHub',
      subtitle: 'Education Platform',
      description: 'Open learning management system designed for South African schools and universities.',
      avatar: 'EH',
      tags: ['Education', 'LMS'],
      stars: '387',
      updated: '1 month ago',
    },
  ];

  const filters = ['All', 'Web', 'Mobile', 'Data', 'Tools'];

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Navigation Pill */}
      <nav className="fixed top-4 left-1/2 transform -translate-x-1/2 z-50 bg-gray-900 text-white rounded-full px-6 py-3 flex items-center gap-6 shadow-lg max-w-fit">
        <div className="font-bold text-lg tracking-tight">Code Masters</div>
        <div className="bg-green-500 text-white px-4 py-2 rounded-full text-sm font-semibold flex items-center gap-2">
          <div className="w-4 h-4 bg-white/30 rounded-full"></div>
          Discover & Explore
        </div>
        <a href="/projects" className="bg-white text-gray-900 px-4 py-2 rounded-full text-sm font-semibold hover:bg-green-500 hover:text-white transition-all">
          Browse Projects
        </a>
        <a href="/impact" className="text-white/90 px-4 py-2 rounded-full text-sm font-semibold hover:bg-white/10 transition-all">
          Impact
        </a>
      </nav>

      {/* Sidebar Toggle */}
      <div className="fixed left-6 top-1/2 transform -translate-y-1/2 w-10 h-10 bg-white border border-gray-300 rounded-full flex items-center justify-center cursor-pointer z-40 hover:shadow-md transition-all">
        ←
      </div>

      {/* Main Container */}
      <div className="max-w-6xl mx-auto p-6 pt-32">
        {/* Hero Section */}
        <section className="bg-white rounded-lg p-12 mb-12 border border-gray-200">
          <h1 className="text-5xl font-bold leading-tight mb-4">
            The front door to South African <span className="text-green-500">open-source</span> discovery
          </h1>
          <p className="text-lg text-gray-600 mb-6 leading-relaxed max-w-2xl">
            Explore projects, connect with developers, and discover the innovation happening across South Africa&apos;s open-source communities.
          </p>
          <a
            href="/projects"
            className="inline-flex items-center gap-2 bg-green-500 text-white font-semibold px-6 py-3 rounded hover:bg-green-600 transition-all"
          >
            Explore Projects
            <ChevronRight size={18} />
          </a>
        </section>

        {/* Page Header with Stats */}
        <div className="flex items-center justify-between mb-8 flex-wrap gap-4">
          <h2 className="text-3xl font-bold">Projects</h2>
          <div className="flex gap-8">
            <div className="text-center">
              <div className="text-3xl font-bold text-green-500">500+</div>
              <div className="text-xs text-gray-600 font-semibold uppercase tracking-wider">Projects</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-gray-900">15K+</div>
              <div className="text-xs text-gray-600 font-semibold uppercase tracking-wider">Developers</div>
            </div>
          </div>
        </div>

        {/* Featured Projects Section */}
        <section className="mb-12">
          <div className="flex items-center justify-between mb-6 flex-wrap gap-4">
            <div>
              <h3 className="text-lg font-bold">
                Featured Projects <span className="text-gray-600 font-normal text-sm">5 projects</span>
              </h3>
            </div>
            <div className="flex gap-2 flex-wrap">
              {filters.map((filter) => (
                <button
                  key={filter}
                  onClick={() => setActiveFilter(filter.toLowerCase())}
                  className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
                    activeFilter === filter.toLowerCase()
                      ? 'bg-green-500 text-white'
                      : 'bg-white text-gray-700 border border-gray-300 hover:bg-green-500 hover:text-white'
                  }`}
                >
                  {filter}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {projects.map((project) => (
              <div
                key={project.id}
                className={`rounded-lg p-6 border transition-all cursor-pointer ${
                  project.featured
                    ? 'bg-gradient-to-br from-green-500 to-green-600 text-white border-none'
                    : 'bg-white border-gray-200 hover:shadow-lg hover:border-green-500'
                }`}
              >
                <div className="flex items-start gap-4 mb-4">
                  <div className={`w-12 h-12 rounded-full flex items-center justify-center font-bold text-lg ${
                    project.featured ? 'bg-white/30' : 'bg-gradient-to-br from-green-500 to-green-600 text-white'
                  }`}>
                    {project.avatar}
                  </div>
                  <div className="flex-1">
                    <h4 className="font-bold text-lg">{project.title}</h4>
                    <p className={`text-sm ${project.featured ? 'text-white/90' : 'text-gray-600'}`}>
                      {project.subtitle}
                    </p>
                  </div>
                </div>

                <p className={`text-sm leading-relaxed mb-4 ${project.featured ? 'text-white/90' : 'text-gray-600'}`}>
                  {project.description}
                </p>

                <div className="flex flex-wrap gap-2 mb-4">
                  {project.tags.map((tag) => (
                    <span
                      key={tag}
                      className={`text-xs px-3 py-1 rounded-full font-medium ${
                        project.featured ? 'bg-white/20 text-white' : 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {tag}
                    </span>
                  ))}
                </div>

                <div className={`flex justify-between text-sm pt-4 border-t ${
                  project.featured ? 'border-white/20 text-white/80' : 'border-gray-200 text-gray-600'
                }`}>
                  <span>⭐ {project.stars} stars</span>
                  <span>{project.updated}</span>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* More Projects Section */}
        <section>
          <div className="flex items-center justify-between mb-6 flex-wrap gap-4">
            <h3 className="text-lg font-bold">
              Explore More <span className="text-gray-600 font-normal text-sm">495+ projects</span>
            </h3>
            <a
              href="/projects"
              className="px-4 py-2 bg-white text-gray-700 border border-gray-300 rounded-full text-sm font-medium hover:bg-green-500 hover:text-white transition-all"
            >
              View All →
            </a>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {moreProjects.map((project) => (
              <div
                key={project.id}
                className="bg-white rounded-lg p-6 border border-gray-200 transition-all hover:shadow-lg hover:border-green-500 cursor-pointer"
              >
                <div className="flex items-start gap-4 mb-4">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-br from-green-500 to-green-600 text-white flex items-center justify-center font-bold text-lg">
                    {project.avatar}
                  </div>
                  <div className="flex-1">
                    <h4 className="font-bold text-lg text-gray-900">{project.title}</h4>
                    <p className="text-sm text-gray-600">{project.subtitle}</p>
                  </div>
                </div>

                <p className="text-sm text-gray-600 leading-relaxed mb-4">{project.description}</p>

                <div className="flex flex-wrap gap-2 mb-4">
                  {project.tags.map((tag) => (
                    <span key={tag} className="text-xs px-3 py-1 bg-gray-100 text-gray-700 rounded-full font-medium">
                      {tag}
                    </span>
                  ))}
                </div>

                <div className="flex justify-between text-sm pt-4 border-t border-gray-200 text-gray-600">
                  <span>⭐ {project.stars} stars</span>
                  <span>{project.updated}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}