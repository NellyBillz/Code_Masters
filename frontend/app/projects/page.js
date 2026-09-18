"use client";

import { useState } from "react";

export default function Projects() {
    const [filters, setFilters] = useState({
        q: "",
        language: "",
        category: "",
        country: "",
        hasBeginnerIssues: false,
        sort: "relevance"
    });

    return (
        <div>
            <h1>Projects</h1>

            {/* 1. Search */}
            <div>
                <label htmlFor="search">Search</label>
                <input
                    id="search"
                    type="text"
                    placeholder="Search projects..."
                    value={filters.q}
                    onChange={(e) =>
                        setFilters({
                            ...filters,
                            q: e.target.value
                        })
                    }
                />
            </div>

            {/* 2. Language */}
            <div>
                <label htmlFor="language">Language</label>
                <select
                    id="language"
                    value={filters.language}
                    onChange={(e) =>
                        setFilters({
                            ...filters,
                            language: e.target.value
                        })
                    }
                >
                    <option value="">All languages</option>
                    <option value="Java">Java</option>
                    <option value="Python">Python</option>
                    <option value="JavaScript">JavaScript</option>
                    <option value="TypeScript">TypeScript</option>
                    <option value="C++">C++</option>
                </select>
            </div>

            {/* 3. Category */}
            <div>
                <label htmlFor="category">Category</label>
                <select
                    id="category"
                    value={filters.category}
                    onChange={(e) =>
                        setFilters({
                            ...filters,
                            category: e.target.value
                        })
                    }
                >
                    <option value="">All categories</option>
                    <option value="Web">Web</option>
                    <option value="Mobile">Mobile</option>
                    <option value="AI">AI</option>
                    <option value="Backend">Backend</option>
                    <option value="Game">Game</option>
                </select>
            </div>

            {/* 4. Country — values are ISO 3166-1 alpha-2 codes, matching
                what the real API stores/filters on. Labels stay as full
                names for display only. */}
            <div>
                <label htmlFor="country">Country</label>
                <select
                    id="country"
                    value={filters.country}
                    onChange={(e) =>
                        setFilters({
                            ...filters,
                            country: e.target.value
                        })
                    }
                >
                    <option value="">All countries</option>
                    <option value="ZA">South Africa</option>
                    <option value="CH">Switzerland</option>
                    <option value="US">United States</option>
                    <option value="GB">United Kingdom</option>
                    <option value="DE">Germany</option>
                </select>
            </div>

            {/* 5. Beginner Issues */}
            <div>
                <label>
                    <input
                        type="checkbox"
                        checked={filters.hasBeginnerIssues}
                        onChange={(e) =>
                            setFilters({
                                ...filters,
                                hasBeginnerIssues: e.target.checked
                            })
                        }
                    />
                    Has beginner issues
                </label>
            </div>

            {/* 6. Sort */}
            <div>
                <label htmlFor="sort">Sort by</label>
                <select
                    id="sort"
                    value={filters.sort}
                    onChange={(e) =>
                        setFilters({
                            ...filters,
                            sort: e.target.value
                        })
                    }
                >
                    <option value="relevance">Relevance</option>
                    <option value="recent">Recent</option>
                    <option value="stars">Stars</option>
                    <option value="contributors">Contributors</option>
                </select>
            </div>

            {/* Debug panel */}
            <div>
                <h2>Current Filters</h2>
                <pre>
                    {JSON.stringify(filters, null, 2)}
                </pre>
            </div>
        </div>
    );
}