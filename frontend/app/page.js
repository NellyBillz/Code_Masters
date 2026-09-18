import Link from "next/link";
import styles from "./page.module.css";

const nodes = [
  { x: 70, y: 60, label: "GH", pulse: false },
  { x: 230, y: 40, label: "EG", pulse: false },
  { x: 330, y: 150, label: "KE", pulse: true },
  { x: 110, y: 220, label: "NG", pulse: true },
  { x: 260, y: 300, label: "ZA", pulse: true, primary: true },
  { x: 180, y: 340, label: null, pulse: false },
];

const edges = [
  [0, 1],
  [0, 3],
  [1, 2],
  [3, 4],
  [2, 4],
  [3, 5],
  [4, 5],
];

export default function Home() {
  return (
    <div className={styles.page}>
      <div className={styles.wordmark}>Code Masters</div>

      <section className={styles.hero}>
        <div className={styles.heroText}>
          <h1 className={styles.headline}>
            Africa&rsquo;s open-source work is happening. Most of it just
            isn&rsquo;t visible yet.
          </h1>
          <p className={styles.subcopy}>
            Code Masters maps South African and African-led projects that
            need contributors, explains what kind of help they&rsquo;re
            looking for, and gets you talking to maintainers before you
            touch a single line of code.
          </p>
          <Link href="/projects" className={styles.cta}>
            Browse open projects
          </Link>
        </div>

        <div className={styles.graphicWrap}>
          <svg
            className={styles.graphic}
            viewBox="0 0 400 400"
            role="img"
            aria-label="A network of open-source projects connected across several African countries"
          >
            <g stroke="#2c405c" strokeWidth="1">
              {edges.map(([a, b], i) => (
                <line
                  key={i}
                  x1={nodes[a].x}
                  y1={nodes[a].y}
                  x2={nodes[b].x}
                  y2={nodes[b].y}
                />
              ))}
            </g>
            <g>
              {nodes.map((n, i) => (
                <g key={i}>
                  <circle
                    cx={n.x}
                    cy={n.y}
                    r={n.primary ? 7 : 5}
                    fill={n.primary ? "var(--accent)" : "var(--accent-green)"}
                    className={n.pulse ? styles.nodePulse : undefined}
                  />
                  {n.label && (
                    <text
                      x={n.x + 12}
                      y={n.y + 4}
                      className={styles.nodeLabel}
                    >
                      {n.label}
                    </text>
                  )}
                </g>
              ))}
            </g>
          </svg>
        </div>
      </section>

      <section className={styles.steps}>
        <div className={styles.stepsInner}>
          <div className={styles.step}>
            <div className={styles.stepNumber}>1</div>
            <h2 className={styles.stepTitle}>Discover</h2>
            <p className={styles.stepBody}>
              Find projects built by developers across the continent,
              filtered by language, country, and the kind of help they
              need.
            </p>
          </div>
          <div className={styles.step}>
            <div className={styles.stepNumber}>2</div>
            <h2 className={styles.stepTitle}>Understand</h2>
            <p className={styles.stepBody}>
              Read the discussion, see who&rsquo;s already looking at an
              issue, and ask questions before you commit any time.
            </p>
          </div>
          <div className={styles.step}>
            <div className={styles.stepNumber}>3</div>
            <h2 className={styles.stepTitle}>Contribute</h2>
            <p className={styles.stepBody}>
              When you&rsquo;re ready, every project links straight to its
              real GitHub repository. Code Masters never replaces GitHub
              — it just gets you there faster.
            </p>
          </div>
        </div>
      </section>

      <footer className={styles.footer}>
        <span>Code Masters</span>
        <span>A discovery layer for African open source.</span>
      </footer>
    </div>
  );
}