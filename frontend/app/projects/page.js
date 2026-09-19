"use client";


import { listProjects } from "../../lib/api";
import ProjectCard from "../components/ProjectCard";

const PAGE_SIZE = 20;

export default function Projects() {
    const [filters, setFilters] = useState({
        q: "",
        language: "",
        category: "",
        country: "",
        hasBeginnerIssues: false,
        sort: "relevance"
    });

    const [projects, setProjects] = useState([]);
    const [meta, setMeta] = useState({
        page: 0,
        size: PAGE_SIZE,
        total: 0
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const updateFilter = (key, value) => {
        setFilters((current) => ({
            ...current,
            [key]: value
        }));

        // Any filter/search/sort change starts from the first page.
        setMeta((current) => ({
            ...current,
            page: 0
        }));
    };

    useEffect(() => {
        let cancelled = false;

        async function loadProjects() {
            setLoading(true);
            setError("");

            try {
                const result = await listProjects({
                    page: meta.page,
                    size: PAGE_SIZE,
                    q: filters.q,
                    language: filters.language,
                    category: filters.category,
                    country: filters.country,
                    hasBeginnerIssues: filters.hasBeginnerIssues ? true : undefined,
                    sort: filters.sort
                });

                if (cancelled) {
                    return;
                }

                setProjects(result.items);
                setMeta(result.meta);
            } catch (err) {
                if (cancelled) {
                    return;
                }

                setProjects([]);
                setError(err.message || "Failed to load projects.");
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadProjects();

        return () => {
            cancelled = true;
        };
    }, [
        filters.q,
        filters.language,
        filters.category,
        filters.country,
        filters.hasBeginnerIssues,
        filters.sort,
        meta.page
    ]);

    const totalPages = Math.ceil(meta.total / PAGE_SIZE);

    const canGoPrevious = meta.page > 0;
    const canGoNext = meta.page + 1 < totalPages;

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
                    onChange={(e) => updateFilter("q", e.target.value)}
                />
            </div>

            {/* 2. Language */}
            <div>
                <label htmlFor="language">Language</label>
                <select
                    id="language"
                    value={filters.language}
                    onChange={(e) =>
                        updateFilter("language", e.target.value)
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
                        updateFilter("category", e.target.value)
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

            {/* 4. Country */}
            <div>
                <label htmlFor="country">Country</label>
                <select
                    id="country"
                    value={filters.country}
                    onChange={(e) =>
                        updateFilter("country", e.target.value)
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
                            updateFilter(
                                "hasBeginnerIssues",
                                e.target.checked
                            )
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
                        updateFilter("sort", e.target.value)
                    }
                >
                    <option value="relevance">Relevance</option>
                    <option value="recent">Recent</option>
                    <option value="stars">Stars</option>
                    <option value="contributors">Contributors</option>
                </select>
            </div>

            {/* Results */}
            <section>
                <h2>Projects</h2>

                {loading && <p>Loading projects...</p>}

                {error && <p role="alert">{error}</p>}

                {!loading && !error && projects.length > 0 && (
                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {projects.map((project) => (
                            <ProjectCard key={project.id} project={project} />
                        ))}
                    </div>
                )}
 
                    ))
            </section>

            {/* Pagination */}
            <section>
                <button
                    type="button"
                    disabled={!canGoPrevious || loading}
                    onClick={() =>
                        setMeta((current) => ({
                            ...current,
                            page: current.page - 1
                        }))
                    }
                >
                    Previous
                </button>

                <span>
                    Page {meta.page + 1} of {Math.max(totalPages, 1)}
                </span>

                <button
                    type="button"
                    disabled={!canGoNext || loading}
                    onClick={() =>
                        setMeta((current) => ({
                            ...current,
                            page: current.page + 1
                        }))
                    }
                >
                    Next
                </button>
            </section>

            {/* Debug panel */}
            <div>
                <h2>Current Filters</h2>
                <pre>
                    {JSON.stringify(filters, null, 2)}
                </pre>
            </div>

            <div>
                <h2>Pagination Meta</h2>
                <pre>
                    {JSON.stringify(meta, null, 2)}
                </pre>
            </div>
        </div>
    );
}