// Course/lab/interview data for Code Masters' Learn section, carried over
// from the original standalone "TechAway" prototype (techaway.jsx). Content
// (lesson text, quiz answers, lab flag values, regex checks) is otherwise
// untouched -- only the "CodeMasters{...}" flag prefix, in-app copy, and the
// color palette import changed (T now comes from ./theme, re-skinned onto
// Code Masters tokens).
import { T } from "./theme";

/* ===================== LEVELS & BADGES ===================== */

const TITLES = ["Boot Sector", "Bit Wrangler", "Loop Lord", "Stack Sage", "Cipher Punk", "Kernel Knight", "Root Legend"];
const levelFromXp = (xp) => Math.floor(Math.sqrt(Math.max(0, xp) / 60)) + 1;
const xpForLevel = (l) => (l - 1) * (l - 1) * 60;
const titleForLevel = (l) => TITLES[Math.min(TITLES.length - 1, Math.floor((l - 1) / 2))];

const BADGES = [
  { id: "first-quest", icon: "🐣", name: "First Quest", desc: "Complete your first challenge" },
  { id: "combo-5", icon: "⚡", name: "Combo x5", desc: "Complete 5 challenges" },
  { id: "quiz-whiz", icon: "🧠", name: "Quiz Whiz", desc: "Ace a chapter quiz" },
  { id: "flag-bearer", icon: "🚩", name: "Flag Bearer", desc: "Capture your first lab flag" },
  { id: "lab-rat", icon: "🥼", name: "Lab Rat", desc: "Clear an entire cyber lab" },
  { id: "streak-3", icon: "🔥", name: "On Fire", desc: "Reach a 3-day streak" },
  { id: "level-5", icon: "🏆", name: "High Five", desc: "Reach level 5" },
  { id: "polyglot", icon: "🌐", name: "Polyglot", desc: "Earn XP in 3 different courses" },
];

/* ===================== COURSE DATA ===================== */
/* Challenge types:
   js      -> user code actually runs; checked against expected console output
   pattern -> code checked against regexes (Python/SQL) + optional AI review
   html    -> live preview in sandboxed iframe; checked against regexes
   text    -> free-text answer                                              */

const COURSES = [
  {
    id: "python",
    name: "Python",
    icon: "🐍",
    tint: T.amber,
    tagline: "The friendliest first language",
    desc: "Variables, logic, and loops — the foundation of everything you'll build.",
    chapters: [
      {
        id: "py-1",
        name: "Chapter 1 · First Spells",
        lessons: [
          {
            id: "py-hello",
            title: "Hello, World!",
            minutes: 4,
            content: [
              { t: "p", v: "Every programmer's journey begins with making the computer say something. In Python, the print() function displays text on the screen." },
              { t: "code", v: 'print("Hello, World!")' },
              { t: "p", v: "Text wrapped in quotes is called a string. Python reads your file top to bottom and runs each line in order." },
              { t: "tip", v: "Python is picky about quotes: open one, close one. \"Hello and \"Hello\" are not the same thing." },
            ],
            challenge: {
              type: "pattern",
              prompt: "Write a program that prints exactly: Hello, CodeMasters!",
              starter: '# your code here\n',
              mustMatch: ["print\\s*\\(\\s*[\"']Hello,\\s*CodeMasters![\"']\\s*\\)"],
              hint: "Use print() and put Hello, CodeMasters! inside quotes.",
            },
          },
          {
            id: "py-vars",
            title: "Variables",
            minutes: 6,
            content: [
              { t: "p", v: "Variables are labelled boxes for storing data. You create one with a name, an equals sign, and a value." },
              { t: "code", v: 'name = "Ada"\nage = 36\nprint(name)\nprint(age)' },
              { t: "p", v: "Strings go in quotes; numbers don't. You can change what's in the box at any time — that's why it's called a variable." },
              { t: "tip", v: "Names can't start with a number and can't contain spaces. snake_case is the Python way." },
            ],
            challenge: {
              type: "pattern",
              prompt: "Create a variable called power set to 9001, then print it.",
              starter: "# create the variable, then print it\n",
              mustMatch: ["power\\s*=\\s*9001", "print\\s*\\(\\s*power\\s*\\)"],
              hint: "Two lines: power = 9001, then print(power).",
            },
          },
          {
            id: "py-if",
            title: "Making Decisions",
            minutes: 7,
            content: [
              { t: "p", v: "Programs get interesting when they choose. An if statement runs code only when a condition is true." },
              { t: "code", v: 'hp = 20\nif hp <= 0:\n    print("Game over")\nelse:\n    print("Keep fighting!")' },
              { t: "p", v: "The indented block belongs to the if. Python uses indentation instead of curly braces — 4 spaces is the convention." },
              { t: "tip", v: "== compares two values. = assigns. Mixing them up is the classic beginner bug." },
            ],
            challenge: {
              type: "pattern",
              prompt: "Write an if statement: if score is greater than 100, print \"Legendary\".",
              starter: "score = 150\n# your if statement here\n",
              mustMatch: ["if\\s+score\\s*>\\s*100\\s*:", "print\\s*\\(\\s*[\"']Legendary[\"']\\s*\\)"],
              hint: "if score > 100: then an indented print on the next line.",
            },
          },
          {
            id: "py-loops",
            title: "Loops",
            minutes: 8,
            content: [
              { t: "p", v: "Loops repeat work so you don't have to. A for loop with range(n) runs a block n times." },
              { t: "code", v: 'for i in range(3):\n    print("Respawn", i)\n# Respawn 0\n# Respawn 1\n# Respawn 2' },
              { t: "p", v: "range(3) counts 0, 1, 2 — computers start counting at zero. The loop variable i takes each value in turn." },
              { t: "tip", v: "Loops + variables = real programs. Try summing numbers or building a countdown." },
            ],
            challenge: {
              type: "pattern",
              prompt: "Write a for loop that prints \"pwned\" 5 times using range.",
              starter: "# loop 5 times\n",
              mustMatch: ["for\\s+\\w+\\s+in\\s+range\\s*\\(\\s*5\\s*\\)\\s*:", "print\\s*\\(\\s*[\"']pwned[\"']\\s*\\)"],
              hint: "for i in range(5): then print(\"pwned\") indented.",
            },
          },
        ],
        quiz: {
          id: "py-quiz-1",
          questions: [
            { q: "What does print(\"2 + 2\") output?", options: ["4", "2 + 2", "Error", "22"], answer: 1, why: "Quotes make it a string — Python prints the characters, not the math." },
            { q: "Which is a valid variable name?", options: ["2fast", "my score", "high_score", "class"], answer: 2, why: "No leading digits, no spaces, no reserved words. snake_case wins." },
            { q: "What does range(4) produce?", options: ["1,2,3,4", "0,1,2,3", "0,1,2,3,4", "4,3,2,1"], answer: 1, why: "range(n) starts at 0 and stops before n." },
            { q: "Which symbol compares equality?", options: ["=", "==", "=>", "!="], answer: 1, why: "= assigns, == compares, != means not equal." },
          ],
        },
      },
    ],
  },
  {
    id: "javascript",
    name: "JavaScript",
    icon: "⚡",
    tint: T.violet,
    tagline: "The language of the web — runs live in your browser",
    desc: "Your code actually executes here. Break things. That's the point.",
    chapters: [
      {
        id: "js-1",
        name: "Chapter 1 · Live Wire",
        lessons: [
          {
            id: "js-log",
            title: "console.log",
            minutes: 4,
            content: [
              { t: "p", v: "JavaScript runs inside every browser on Earth. console.log() is how you make it talk — it prints to the developer console." },
              { t: "code", v: 'console.log("Hello, web!");\nconsole.log(40 + 2);' },
              { t: "p", v: "In this course your code genuinely runs in a sandbox. What you see in the output panel is real execution, not a simulation." },
              { t: "tip", v: "Semicolons end statements. JavaScript often forgives you if you forget them — but don't rely on its mercy." },
            ],
            challenge: {
              type: "js",
              prompt: "Log the exact text: CodeMasters online",
              starter: "// make it talk\n",
              expectOutput: ["CodeMasters online"],
              hint: 'console.log("CodeMasters online");',
            },
          },
          {
            id: "js-vars",
            title: "let & const",
            minutes: 6,
            content: [
              { t: "p", v: "Modern JavaScript has two ways to declare variables: let for values that change, const for values that don't." },
              { t: "code", v: 'const player = "Nova";\nlet score = 0;\nscore = score + 50;\nconsole.log(player, score); // Nova 50' },
              { t: "p", v: "Try to reassign a const and JavaScript throws an error — a feature, not a bug. It protects you from accidental overwrites." },
              { t: "tip", v: "Default to const. Reach for let only when you know the value will change." },
            ],
            challenge: {
              type: "js",
              prompt: "Declare let coins = 10, add 5 to it, then log coins. Expected output: 15",
              starter: "// declare, add, log\n",
              expectOutput: ["15"],
              hint: "let coins = 10; coins = coins + 5; console.log(coins);",
            },
          },
          {
            id: "js-func",
            title: "Functions",
            minutes: 8,
            content: [
              { t: "p", v: "Functions are reusable spells: define once, cast anywhere. They take inputs (parameters) and can return a result." },
              { t: "code", v: "function double(n) {\n  return n * 2;\n}\nconsole.log(double(21)); // 42" },
              { t: "p", v: "return hands a value back to whoever called the function. Without it, a function returns undefined." },
              { t: "tip", v: "Name functions after what they do: double, isEven, launchMissiles... okay, maybe not that last one." },
            ],
            challenge: {
              type: "js",
              prompt: "Write a function triple(n) that returns n * 3, then log triple(7). Expected output: 21",
              starter: "// define triple, then log triple(7)\n",
              expectOutput: ["21"],
              hint: "function triple(n) { return n * 3; } console.log(triple(7));",
            },
          },
          {
            id: "js-arrays",
            title: "Arrays & Loops",
            minutes: 9,
            content: [
              { t: "p", v: "Arrays hold ordered lists of values. Loop through them to process each item." },
              { t: "code", v: 'const loot = ["sword", "shield", "potion"];\nfor (const item of loot) {\n  console.log(item);\n}' },
              { t: "p", v: "loot[0] is the first item — indexing starts at zero. loot.length tells you how many items there are." },
              { t: "tip", v: "for...of is the cleanest way to visit every element. Use a classic for (let i = 0; ...) when you need the index." },
            ],
            challenge: {
              type: "js",
              prompt: "Given const nums = [2, 4, 6], loop through and log each number times 10. Expected output: 20, 40, 60 (each on its own line)",
              starter: "const nums = [2, 4, 6];\n// loop and log n * 10\n",
              expectOutput: ["20", "40", "60"],
              hint: "for (const n of nums) { console.log(n * 10); }",
            },
          },
        ],
        quiz: {
          id: "js-quiz-1",
          questions: [
            { q: "Which declaration cannot be reassigned?", options: ["let", "var", "const", "static"], answer: 2, why: "const locks the binding — reassigning throws an error." },
            { q: "What does [10, 20, 30][1] evaluate to?", options: ["10", "20", "30", "1"], answer: 1, why: "Index 1 is the second element. Arrays start at 0." },
            { q: "A function without a return statement returns…", options: ["0", "null", "undefined", "the last value"], answer: 2, why: "No return means undefined comes back." },
            { q: "console.log(2 + \"2\") prints…", options: ["4", "22", "Error", "NaN"], answer: 1, why: "Number + string = string concatenation. JavaScript keeps you humble." },
          ],
        },
      },
    ],
  },
  {
    id: "htmlcss",
    name: "HTML & CSS",
    icon: "🎨",
    tint: T.coral,
    tagline: "Build pages you can actually see",
    desc: "Structure with HTML, style with CSS — with a live preview as you type.",
    chapters: [
      {
        id: "web-1",
        name: "Chapter 1 · Pixels & Tags",
        lessons: [
          {
            id: "web-tags",
            title: "Your First Page",
            minutes: 5,
            content: [
              { t: "p", v: "HTML describes a page's structure using tags. Most tags open and close: <h1>...</h1> is a heading, <p>...</p> is a paragraph." },
              { t: "code", v: "<h1>Welcome to my base</h1>\n<p>Population: me.</p>" },
              { t: "p", v: "In this course the editor renders a live preview — every keystroke updates the page on the right." },
              { t: "tip", v: "h1 through h6 are heading levels. One h1 per page is good practice." },
            ],
            challenge: {
              type: "html",
              prompt: "Create an <h1> that says CodeMasters HQ and a <p> with any text.",
              starter: "<!-- build your page -->\n",
              mustMatch: ["<h1>\\s*CodeMasters HQ\\s*</h1>", "<p>[\\s\\S]*?</p>"],
              hint: "<h1>CodeMasters HQ</h1> then <p>anything here</p>",
            },
          },
          {
            id: "web-lists",
            title: "Lists & Links",
            minutes: 6,
            content: [
              { t: "p", v: "Unordered lists (<ul>) hold bullet items (<li>). Links use the <a> tag with an href attribute pointing somewhere." },
              { t: "code", v: '<ul>\n  <li>Learn HTML</li>\n  <li>Hack the planet (legally)</li>\n</ul>\n<a href="https://example.com">Visit base camp</a>' },
              { t: "p", v: "Attributes live inside the opening tag and configure it. href tells a link where to go." },
              { t: "tip", v: "Nesting matters: <li> belongs inside <ul> or <ol>, never floating on its own." },
            ],
            challenge: {
              type: "html",
              prompt: "Build a <ul> containing at least two <li> items.",
              starter: "<!-- your inventory list -->\n",
              mustMatch: ["<ul>[\\s\\S]*<li>[\\s\\S]*</li>[\\s\\S]*<li>[\\s\\S]*</li>[\\s\\S]*</ul>"],
              hint: "<ul> <li>one</li> <li>two</li> </ul>",
            },
          },
          {
            id: "web-css",
            title: "Enter CSS",
            minutes: 8,
            content: [
              { t: "p", v: "CSS controls how things look. A rule has a selector (what to style) and declarations (how to style it) inside curly braces." },
              { t: "code", v: "<style>\n  h1 { color: crimson; }\n  p { font-size: 20px; }\n</style>\n<h1>Styled!</h1>\n<p>Fancy paragraph.</p>" },
              { t: "p", v: "Put CSS inside a <style> tag for now. Each declaration is property: value; — don't forget the semicolon." },
              { t: "tip", v: "color styles text; background-color styles the box behind it." },
            ],
            challenge: {
              type: "html",
              prompt: "Style an h1 with color: gold using a <style> tag, and include an <h1> element.",
              starter: "<style>\n  /* your rule */\n</style>\n\n",
              mustMatch: ["h1\\s*\\{[^}]*color\\s*:\\s*gold", "<h1>[\\s\\S]*?</h1>"],
              hint: "h1 { color: gold; } inside <style>, plus an <h1> below it.",
            },
          },
        ],
        quiz: {
          id: "web-quiz-1",
          questions: [
            { q: "Which tag creates the biggest heading?", options: ["<head>", "<h6>", "<h1>", "<big>"], answer: 2, why: "h1 is the top-level heading; <head> is metadata, not visible text." },
            { q: "Where does the href attribute belong?", options: ["<p>", "<a>", "<li>", "<style>"], answer: 1, why: "href gives an anchor tag its destination." },
            { q: "In CSS, h1 { color: red; } — what is h1 called?", options: ["property", "value", "selector", "attribute"], answer: 2, why: "The selector picks which elements the rule applies to." },
          ],
        },
      },
    ],
  },
  {
    id: "sql",
    name: "SQL",
    icon: "🗄️",
    tint: T.cyan,
    tagline: "Talk to databases like a boss",
    desc: "Every app has data behind it. SQL is how you ask that data questions.",
    chapters: [
      {
        id: "sql-1",
        name: "Chapter 1 · SELECT Star",
        lessons: [
          {
            id: "sql-select",
            title: "SELECT & FROM",
            minutes: 5,
            content: [
              { t: "p", v: "Databases store data in tables — like spreadsheets with superpowers. SQL queries fetch rows from them." },
              { t: "code", v: "SELECT * FROM players;\n-- grabs every column of every row in players" },
              { t: "p", v: "SELECT picks columns, FROM names the table. The star * means all columns. Statements end with a semicolon." },
              { t: "tip", v: "SQL keywords aren't case-sensitive, but SHOUTING THEM is tradition — it makes queries readable." },
            ],
            challenge: {
              type: "pattern",
              prompt: "Write a query that selects all columns from a table called agents.",
              starter: "-- your query\n",
              mustMatch: ["select\\s+\\*\\s+from\\s+agents\\s*;?"],
              flags: "i",
              hint: "SELECT * FROM agents;",
            },
          },
          {
            id: "sql-where",
            title: "Filtering with WHERE",
            minutes: 7,
            content: [
              { t: "p", v: "WHERE filters rows by a condition — only matching rows come back." },
              { t: "code", v: "SELECT name, score FROM players\nWHERE score > 9000;" },
              { t: "p", v: "You can also pick specific columns instead of *. Conditions use =, >, <, >=, <=, and <> (not equal). Text values go in single quotes." },
              { t: "tip", v: "WHERE role = 'admin' — single quotes for strings in SQL, unlike most programming languages." },
            ],
            challenge: {
              type: "pattern",
              prompt: "Select the name column from agents where clearance = 5.",
              starter: "-- filter it down\n",
              mustMatch: ["select\\s+name\\s+from\\s+agents\\s+where\\s+clearance\\s*=\\s*5"],
              flags: "i",
              hint: "SELECT name FROM agents WHERE clearance = 5;",
            },
          },
          {
            id: "sql-order",
            title: "Sorting & Limiting",
            minutes: 6,
            content: [
              { t: "p", v: "ORDER BY sorts results; LIMIT caps how many rows return. Together they build leaderboards." },
              { t: "code", v: "SELECT name, xp FROM players\nORDER BY xp DESC\nLIMIT 10;" },
              { t: "p", v: "DESC sorts high-to-low, ASC (the default) low-to-high. This exact shape of query powers the Code Masters leaderboard." },
              { t: "tip", v: "Clause order matters: SELECT → FROM → WHERE → ORDER BY → LIMIT." },
            ],
            challenge: {
              type: "pattern",
              prompt: "Get name and xp from players, sorted by xp descending, limited to 3 rows.",
              starter: "-- top three\n",
              mustMatch: ["select\\s+name\\s*,\\s*xp\\s+from\\s+players", "order\\s+by\\s+xp\\s+desc", "limit\\s+3"],
              flags: "i",
              hint: "SELECT name, xp FROM players ORDER BY xp DESC LIMIT 3;",
            },
          },
        ],
        quiz: {
          id: "sql-quiz-1",
          questions: [
            { q: "What does SELECT * mean?", options: ["Select nothing", "Select all columns", "Select all tables", "Multiply results"], answer: 1, why: "The star is a wildcard for every column." },
            { q: "Which clause filters rows?", options: ["ORDER BY", "FROM", "WHERE", "LIMIT"], answer: 2, why: "WHERE keeps only rows matching the condition." },
            { q: "How do you write text values in SQL?", options: ["\"double quotes\"", "'single quotes'", "`backticks`", "no quotes"], answer: 1, why: "Standard SQL uses single quotes for string literals." },
          ],
        },
      },
    ],
  },
  {
    id: "cyber",
    name: "Cybersecurity",
    icon: "🛡️",
    tint: T.green,
    tagline: "Think like an attacker, defend like a pro",
    desc: "Core security concepts — then prove it in the hands-on labs.",
    chapters: [
      {
        id: "cy-1",
        name: "Chapter 1 · Security Mindset",
        lessons: [
          {
            id: "cy-cia",
            title: "The CIA Triad",
            minutes: 6,
            content: [
              { t: "p", v: "All of security orbits three goals: Confidentiality (only the right people can read data), Integrity (data isn't tampered with), and Availability (systems work when needed)." },
              { t: "code", v: "Confidentiality → encryption, access control\nIntegrity       → hashing, signatures\nAvailability    → backups, redundancy" },
              { t: "p", v: "Every attack breaks at least one of the three. A data leak breaks confidentiality; defacing a site breaks integrity; knocking a server offline breaks availability." },
              { t: "tip", v: "When you hear about a breach in the news, ask: which leg of the triad failed?" },
            ],
            challenge: {
              type: "text",
              prompt: "A hospital's patient records are encrypted by ransomware and staff can't access them. Which part of the CIA triad is primarily broken? (one word)",
              answer: ["availability"],
              hint: "Staff can't access the systems they need — the data isn't leaked, it's locked.",
            },
          },
          {
            id: "cy-encoding",
            title: "Encoding vs Encryption vs Hashing",
            minutes: 8,
            content: [
              { t: "p", v: "These three get confused constantly. Encoding (like Base64) transforms data for transport — anyone can reverse it, no key needed. It is NOT security." },
              { t: "p", v: "Encryption scrambles data with a key; only key-holders can decrypt. Hashing is a one-way fingerprint: same input always gives the same hash, but you can't run it backwards." },
              { t: "code", v: 'Base64("hi")  → aGk=            (reversible, no key)\nEncrypt("hi") → j8#kQ…          (reversible WITH key)\nSHA-256("hi") → 8f434346648f…   (one-way fingerprint)' },
              { t: "p", v: "Websites store hashes of your password, not the password itself. At login, they hash what you typed and compare fingerprints." },
              { t: "tip", v: "Spot Base64 in the wild: ends in = or ==, uses A–Z, a–z, 0–9, +, /. You'll decode some in the Crypto Lab." },
            ],
            challenge: {
              type: "text",
              prompt: "A developer 'protects' passwords by Base64-encoding them. Is this secure? Answer yes or no.",
              answer: ["no"],
              hint: "Encoding needs no key to reverse — anyone can decode Base64 instantly.",
            },
          },
          {
            id: "cy-passwords",
            title: "Passwords & Authentication",
            minutes: 7,
            content: [
              { t: "p", v: "Attackers rarely 'crack' strong passwords — they guess weak ones, reuse leaked ones, or trick you into handing them over. Length beats complexity: a 4-word passphrase outlasts P@ssw0rd1 by centuries of guessing." },
              { t: "code", v: "correct-horse-battery-staple  → decades to brute-force\nP@ssw0rd1                     → seconds (it's in every wordlist)" },
              { t: "p", v: "Multi-factor authentication (MFA) adds a second proof — something you have (phone, key) on top of something you know. Even a stolen password fails without the second factor." },
              { t: "tip", v: "Password managers exist so every account can have a unique, random password you never memorize." },
            ],
            challenge: {
              type: "text",
              prompt: "What does MFA stand for? (three words)",
              answer: ["multi-factor authentication", "multi factor authentication", "multifactor authentication"],
              hint: "Something you know + something you have.",
            },
          },
          {
            id: "cy-social",
            title: "Social Engineering",
            minutes: 7,
            content: [
              { t: "p", v: "The easiest system to hack is a human. Social engineering manipulates people into breaking security themselves — phishing emails, fake IT calls, urgent 'CEO' requests." },
              { t: "p", v: "The red flags repeat: urgency ('act NOW'), authority ('this is the CEO'), unusual channels, requests for credentials or payment, and links whose real domain doesn't match the brand." },
              { t: "code", v: "Legit:  https://accounts.google.com/signin\nPhish:  https://accounts.google.verify-login.ru/signin\n         ↑ the REAL domain is verify-login.ru" },
              { t: "p", v: "Read domains right-to-left from the first single slash: the last two segments are who actually owns the site. Everything before can be faked." },
              { t: "tip", v: "You'll put this to work in the Phish Market lab — real-looking emails, your call." },
            ],
            challenge: {
              type: "text",
              prompt: "In the URL https://paypal.com.secure-billing.net/login — what is the real registered domain? (two segments, like example.com)",
              answer: ["secure-billing.net"],
              hint: "Read right to left from the first slash: net, then secure-billing.",
            },
          },
        ],
        quiz: {
          id: "cy-quiz-1",
          questions: [
            { q: "Hashing is best described as…", options: ["Reversible with a key", "A one-way fingerprint", "The same as encoding", "A type of firewall"], answer: 1, why: "Hashes can't be run backwards — that's the whole point." },
            { q: "Which is the STRONGEST password?", options: ["P@ssw0rd!", "qwerty123", "lamp-orbit-cactus-nine", "hunter2"], answer: 2, why: "Long random passphrases beat short 'complex' ones." },
            { q: "An email says 'URGENT: verify your account in 1 hour or lose access'. This pressure tactic is…", options: ["Encryption", "A brute-force attack", "Social engineering", "A DDoS"], answer: 2, why: "Manufactured urgency is the classic phishing move." },
            { q: "Base64 is…", options: ["Strong encryption", "A hashing algorithm", "Encoding — trivially reversible", "A password manager"], answer: 2, why: "Encoding is for transport, not secrecy. Anyone can decode it." },
          ],
        },
      },
    ],
  },
];

/* ===================== CYBER LABS ===================== */

const TERMINAL_FS = {
  "home": {
    "agent": {
      "README.txt": "Welcome, agent. Somewhere on this machine is a flag: CodeMasters{...}\nHidden things start with a dot. Try: ls -a\nUseful: ls, cd <dir>, cd .., cat <file>, pwd, whoami, help",
      "notes.txt": "todo:\n- change default passwords\n- stop hiding secrets in dotfiles (seriously)",
      "projects": {
        "app.py": "print('nothing to see here')",
        "decoy.txt": "Not the flag. Keep digging. Dotfiles, agent, dotfiles.",
      },
      ".secrets": {
        "flag.txt": "CodeMasters{d0tf1l3s_ar3_n0t_s3cur1ty}",
        "diary.txt": "Day 12: I hid the flag in a dotfile. Nobody will EVER find it.",
      },
    },
  },
};

const LABS = [
  {
    id: "lab-terminal",
    name: "Terminal Basics",
    icon: "🖥️",
    tint: T.green,
    difficulty: "Easy",
    desc: "A real interactive Linux-style shell. Navigate the filesystem, uncover hidden files, capture the flag.",
    tasks: [
      "Run ls to look around, and cat README.txt for your briefing",
      "Explore directories with cd — check the projects folder",
      "Hidden files start with a dot. ls -a reveals them",
      "cat the flag file and submit CodeMasters{...} below",
    ],
    flags: [{ id: "term-1", label: "Hidden flag", value: "CodeMasters{d0tf1l3s_ar3_n0t_s3cur1ty}", xp: 60 }],
  },
  {
    id: "lab-crypto",
    name: "Crypto Crackdown",
    icon: "🔐",
    tint: T.amber,
    difficulty: "Medium",
    desc: "Three intercepted messages, three classic schemes: Base64, Caesar cipher, and a reversal. Use the built-in decoder tools.",
    tasks: [
      "Message 1 ends with = — that smells like Base64. Decode it",
      "Message 2 is a Caesar cipher. Slide the shift until words appear",
      "Message 3 reads like nonsense… or does it, backwards?",
      "Each decoded message contains a CodeMasters{...} flag",
    ],
    intercepts: [
      { id: "cr-b64", label: "Intercept #1 — captured from HTTP traffic", data: "VGVjaEF3YXl7YjY0X2lzX24wdF8zbmNyeXB0aTBufQ==", method: "base64" },
      { id: "cr-caesar", label: "Intercept #2 — scratched into a desk", data: "WhfkDzdb{fdhvdu_vdodg_lv_ilqh}", method: "caesar", shift: 3 },
      { id: "cr-rev", label: "Intercept #3 — found in a mirror selfie", data: "}drawkcab_kniht{yawAhceT", method: "reverse" },
    ],
    flags: [
      { id: "cr-1", label: "Intercept #1 flag", value: "CodeMasters{b64_is_n0t_3ncrypti0n}", xp: 40 },
      { id: "cr-2", label: "Intercept #2 flag", value: "CodeMasters{caesar_salad_is_fine}", xp: 40 },
      { id: "cr-3", label: "Intercept #3 flag", value: "CodeMasters{think_backward}", xp: 40 },
    ],
  },
  {
    id: "lab-phish",
    name: "Phish Market",
    icon: "🎣",
    tint: T.coral,
    difficulty: "Easy",
    desc: "Four emails landed in the company inbox. Some are legit, some are bait. Inspect senders, links, and tone — then make the call.",
    tasks: [
      "Check the sender's REAL domain, not the display name",
      "Hover the mental magnifier over every link",
      "Urgency + credentials request = alarm bells",
      "Classify all four correctly to capture the flag",
    ],
    emails: [
      {
        id: "ph-1", from: "IT Support <helpdesk@paypa1-security.com>", subject: "URGENT: Your account will be suspended",
        body: "Dear user, we detected unusual activity. Verify your password within 1 HOUR or your account will be permanently suspended.\n\n→ http://paypa1-security.com/verify\n\nPayPal Security Team",
        phish: true, why: "Look closely: paypa1 with a number 1, not paypal. Plus manufactured urgency and a password request — the full bingo card.",
      },
      {
        id: "ph-2", from: "GitHub <noreply@github.com>", subject: "[GitHub] A new SSH key was added to your account",
        body: "A new SSH key was added to your account. If this was you, no action is needed.\n\nIf you did not add this key, review your security settings at https://github.com/settings/keys",
        phish: false, why: "Real domain, no urgency theatre, no credential request, and it points to the site's own settings page. Standard security notice.",
      },
      {
        id: "ph-3", from: "CEO Sandra Wells <s.wells@gmail.com>", subject: "Quick favour — are you at your desk?",
        body: "I'm heading into a board meeting and need you to buy 5 x $100 gift cards for a client. Send me the codes ASAP. Keep this between us for now.\n\nSent from my iPhone",
        phish: true, why: "The 'CEO' writes from a personal gmail, wants gift card codes, demands speed and secrecy. Classic CEO-fraud, hall-of-fame edition.",
      },
      {
        id: "ph-4", from: "Slack <feedback@slack.com>", subject: "Your workspace invoice for June",
        body: "Hi there, your monthly invoice for workspace 'techaway-hq' is attached. Amount charged to card ending 4242: $96.50.\n\nQuestions? Visit https://slack.com/help",
        phish: false, why: "Matches an expected billing cycle, real domain, no link demanding credentials, no pressure. Routine receipt.",
      },
    ],
    flags: [{ id: "ph-flag", label: "Perfect classification", value: "CodeMasters{trust_n0_display_name}", xp: 60 }],
  },
];


/* ===================== BEGINNER → PRO EXPANSION ===================== */

const PY_CH2 = {
  id: "py-2", name: "Chapter 2 · Data Structures & Functions", tier: "Intermediate",
  lessons: [
    {
      id: "py-lists", title: "Lists", minutes: 7,
      content: [
        { t: "p", v: "Lists hold ordered collections. Create them with square brackets, grab items by index, and grow them with append()." },
        { t: "code", v: 'gear = ["rope", "torch", "map"]\nprint(gear[0])      # rope\ngear.append("sword")\nprint(len(gear))    # 4' },
        { t: "p", v: "Negative indexes count from the end: gear[-1] is the last item. Slicing gear[0:2] takes a sub-list." },
        { t: "tip", v: "Lists are mutable — you can change them after creation. Strings are not." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Create a list called gear with at least 3 items, then print the first item using its index.",
        starter: "# build your inventory\n",
        mustMatch: ["gear\\s*=\\s*\\[[^\\]]+,[^\\]]+,[^\\]]+\\]", "print\\s*\\(\\s*gear\\s*\\[\\s*0\\s*\\]\\s*\\)"],
        hint: 'gear = ["a", "b", "c"] then print(gear[0])',
      },
    },
    {
      id: "py-dicts", title: "Dictionaries", minutes: 8,
      content: [
        { t: "p", v: "Dictionaries map keys to values — like a real dictionary maps words to definitions. Perfect for structured data." },
        { t: "code", v: 'player = {"name": "Ada", "hp": 100, "level": 3}\nprint(player["hp"])   # 100\nplayer["hp"] -= 25    # take damage\nprint(player["hp"])   # 75' },
        { t: "p", v: "Access values with square brackets and the key. Add a new key by assigning to it: player[\"mana\"] = 50." },
        { t: "tip", v: "Asking for a key that doesn't exist raises KeyError. player.get(\"mana\", 0) returns a default instead." },
      ],
      challenge: {
        type: "pattern",
        prompt: 'Create a dict called player with a "name" key and an "hp" key set to 100, then print the hp value.',
        starter: "# spawn your player\n",
        mustMatch: ["player\\s*=\\s*\\{[^}]*[\"']name[\"'][^}]*[\"']hp[\"']\\s*:\\s*100[^}]*\\}", "print\\s*\\(\\s*player\\s*\\[\\s*[\"']hp[\"']\\s*\\]\\s*\\)"],
        hint: 'player = {"name": "Ada", "hp": 100} then print(player["hp"])',
      },
    },
    {
      id: "py-funcs", title: "Functions", minutes: 9,
      content: [
        { t: "p", v: "def creates a reusable function. Parameters go in the parentheses; return hands a value back to the caller." },
        { t: "code", v: "def damage(base, crit):\n    if crit:\n        return base * 2\n    return base\n\nprint(damage(50, True))   # 100" },
        { t: "p", v: "Functions let you name a piece of logic once and reuse it everywhere. Code without functions becomes spaghetti fast." },
        { t: "tip", v: "A function with no return gives back None. If your prints say None, you probably forgot to return." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Define a function square(n) that returns n multiplied by itself.",
        starter: "# def it\n",
        mustMatch: ["def\\s+square\\s*\\(\\s*n\\s*\\)\\s*:", "return\\s+(n\\s*\\*\\s*n|n\\s*\\*\\*\\s*2)"],
        hint: "def square(n): then return n * n indented.",
      },
    },
    {
      id: "py-while", title: "While Loops", minutes: 7,
      content: [
        { t: "p", v: "A while loop repeats as long as its condition stays true — ideal when you don't know how many iterations you need." },
        { t: "code", v: 'hp = 30\nwhile hp > 0:\n    print("Still standing:", hp)\n    hp -= 10\nprint("Down!")' },
        { t: "p", v: "Something inside the loop must move the condition toward false — here hp -= 10 — or you've built an infinite loop." },
        { t: "tip", v: "break exits a loop early; continue skips to the next iteration. Both are legal escape hatches." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Write a while loop that runs while hp > 0 and decreases hp by 10 each pass.",
        starter: "hp = 50\n# countdown to zero\n",
        mustMatch: ["while\\s+hp\\s*>\\s*0\\s*:", "hp\\s*-=\\s*10|hp\\s*=\\s*hp\\s*-\\s*10"],
        hint: "while hp > 0: then hp -= 10 indented (a print in between is nice).",
      },
    },
  ],
  quiz: {
    id: "py-quiz-2",
    questions: [
      { q: 'What does ["a","b","c"][-1] return?', options: ["a", "b", "c", "Error"], answer: 2, why: "-1 indexes from the end — the last element." },
      { q: "How do you read the value for key 'hp' in dict d?", options: ["d.hp", "d('hp')", "d['hp']", "d->hp"], answer: 2, why: "Square brackets with the key — dot access is for objects in other languages." },
      { q: "A function without return gives back…", options: ["0", "None", "'' (empty string)", "the last variable"], answer: 1, why: "Python functions return None unless told otherwise." },
      { q: "Which loop risks running forever?", options: ["for i in range(5)", "while True with no break", "for x in [1,2]", "none of them"], answer: 1, why: "while True never ends unless something breaks out." },
    ],
  },
};

const PY_CH3 = {
  id: "py-3", name: "Chapter 3 · Object-Oriented & Robust", tier: "Pro",
  lessons: [
    {
      id: "py-class", title: "Classes & Objects", minutes: 10,
      content: [
        { t: "p", v: "Classes are blueprints for objects: they bundle data (attributes) and behaviour (methods) together. __init__ runs when an object is created; self refers to the object itself." },
        { t: "code", v: 'class Enemy:\n    def __init__(self, name):\n        self.name = name\n        self.hp = 50\n\n    def hit(self, dmg):\n        self.hp -= dmg\n\ngoblin = Enemy("Grub")\ngoblin.hit(20)\nprint(goblin.hp)   # 30' },
        { t: "p", v: "Every game entity, web request, and database row you'll ever work with is an object under the hood. This is the paradigm most large codebases are built on." },
        { t: "tip", v: "Class names use CapWords; instances and methods use snake_case." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Define a class Enemy whose __init__ sets self.hp = 50.",
        starter: "# blueprint time\n",
        mustMatch: ["class\\s+Enemy\\s*[:(]", "def\\s+__init__\\s*\\(\\s*self[^)]*\\)\\s*:", "self\\.hp\\s*=\\s*50"],
        hint: "class Enemy: → def __init__(self): → self.hp = 50",
      },
    },
    {
      id: "py-except", title: "Errors & Exceptions", minutes: 9,
      content: [
        { t: "p", v: "Pro code expects failure. try/except catches errors at runtime so one bad input doesn't crash the whole program." },
        { t: "code", v: 'try:\n    age = int(input_value)\nexcept ValueError:\n    print("That was not a number")\nfinally:\n    print("Runs no matter what")' },
        { t: "p", v: "Catch the specific exception you expect (ValueError, KeyError, ZeroDivisionError) — a bare except hides bugs you wanted to see." },
        { t: "tip", v: "raise ValueError(\"bad input\") throws your own exception. Good libraries fail loudly and clearly." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Write a try block that attempts int(user_input) and an except ValueError that prints a warning.",
        starter: 'user_input = "not a number"\n# guard it\n',
        mustMatch: ["try\\s*:", "int\\s*\\(\\s*user_input\\s*\\)", "except\\s+ValueError\\s*:", "print\\s*\\("],
        hint: "try: → int(user_input) → except ValueError: → print(...)",
      },
    },
    {
      id: "py-comp", title: "List Comprehensions", minutes: 8,
      content: [
        { t: "p", v: "Comprehensions build lists in one expressive line — the most Pythonic move there is. Read them as: give me THIS, for each of THAT, optionally if CONDITION." },
        { t: "code", v: "squares = [n * n for n in range(5)]\n# [0, 1, 4, 9, 16]\nevens = [n for n in range(10) if n % 2 == 0]\n# [0, 2, 4, 6, 8]" },
        { t: "p", v: "Anything you'd write as a for-loop that appends to a list can usually collapse into a comprehension — clearer intent, fewer lines." },
        { t: "tip", v: "If the comprehension needs more than one line to read comfortably, use a normal loop. Clever is not the goal; clear is." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Build a list comprehension: squares = the square of each n for n in range(5).",
        starter: "# one line, all power\n",
        mustMatch: ["squares\\s*=\\s*\\[\\s*n\\s*(\\*\\s*n|\\*\\*\\s*2)\\s+for\\s+n\\s+in\\s+range\\s*\\(\\s*5\\s*\\)\\s*\\]"],
        hint: "squares = [n * n for n in range(5)]",
      },
    },
  ],
  quiz: {
    id: "py-quiz-3",
    questions: [
      { q: "In a class method, self refers to…", options: ["the class itself", "the current object instance", "the parent class", "Python's runtime"], answer: 1, why: "self is the specific object the method was called on." },
      { q: "Which except clause is bad practice?", options: ["except ValueError:", "except KeyError:", "except: (bare)", "except ZeroDivisionError:"], answer: 2, why: "Bare except swallows every error, including the ones you needed to see." },
      { q: "[n*2 for n in range(3)] equals…", options: ["[0,2,4]", "[2,4,6]", "[0,1,2]", "[1,2,3]"], answer: 0, why: "range(3) is 0,1,2 — doubled gives 0,2,4." },
      { q: "__init__ runs…", options: ["when the file loads", "when the object is created", "when the object is deleted", "only if you call it"], answer: 1, why: "It's the constructor — automatic on instantiation." },
    ],
  },
};

const JS_CH2 = {
  id: "js-2", name: "Chapter 2 · Objects & Array Power", tier: "Intermediate",
  lessons: [
    {
      id: "js-obj", title: "Objects", minutes: 7,
      content: [
        { t: "p", v: "Objects group related data under named keys. They're the backbone of JavaScript — even arrays and functions are objects." },
        { t: "code", v: 'const hero = { name: "Nova", hp: 80, level: 4 };\nconsole.log(hero.name);      // Nova\nhero.hp -= 30;\nconsole.log(hero.hp);        // 50' },
        { t: "p", v: "Dot notation (hero.hp) reads and writes properties. Bracket notation hero[\"hp\"] does the same and allows dynamic keys." },
        { t: "tip", v: "const hero = {...} — you can still change hero's properties. const only locks the binding, not the contents." },
      ],
      challenge: {
        type: "js",
        prompt: "Create const hero = an object with name \"Nova\" and hp 80, then log hero.hp. Expected output: 80",
        starter: "// build your hero\n",
        expectOutput: ["80"],
        hint: 'const hero = { name: "Nova", hp: 80 }; console.log(hero.hp);',
      },
    },
    {
      id: "js-mapfilter", title: "map & filter", minutes: 9,
      content: [
        { t: "p", v: "map transforms every element of an array; filter keeps only the elements that pass a test. Both return a NEW array — the original is untouched." },
        { t: "code", v: "const nums = [1, 2, 3, 4];\nconst doubled = nums.map(n => n * 2);     // [2,4,6,8]\nconst evens  = nums.filter(n => n % 2 === 0); // [2,4]\nconsole.log(doubled.join(\",\"));" },
        { t: "p", v: "The n => n * 2 syntax is an arrow function — a compact function expression. You'll see these everywhere in modern JavaScript." },
        { t: "tip", v: "Chain them: nums.filter(n => n > 1).map(n => n * 10). Data pipelines in one line." },
      ],
      challenge: {
        type: "js",
        prompt: "Given const nums = [1,2,3,4], use map to double each and log the result joined by commas. Expected output: 2,4,6,8",
        starter: "const nums = [1, 2, 3, 4];\n// map, join, log\n",
        expectOutput: ["2,4,6,8"],
        hint: 'console.log(nums.map(n => n * 2).join(","));',
      },
    },
    {
      id: "js-template", title: "Template Literals", minutes: 6,
      content: [
        { t: "p", v: "Backtick strings let you embed expressions with ${...} — no more clunky + concatenation." },
        { t: "code", v: "const name = \"Ada\";\nconst lvl = 7;\nconsole.log(`${name} reached level ${lvl}!`);\n// Ada reached level 7!" },
        { t: "p", v: "Anything inside ${} is evaluated: variables, math, even function calls. Template literals can also span multiple lines." },
        { t: "tip", v: "Backtick ` is not a quote ' — it's usually under the Esc key. Muscle memory takes a week." },
      ],
      challenge: {
        type: "js",
        prompt: "Given const name = \"Ada\", use a template literal to log: Hello, Ada! Expected output exactly: Hello, Ada!",
        starter: 'const name = "Ada";\n// backticks, not quotes\n',
        expectOutput: ["Hello, Ada!"],
        hint: "console.log(`Hello, ${name}!`);",
      },
    },
  ],
  quiz: {
    id: "js-quiz-2",
    questions: [
      { q: "hero.hp and hero[\"hp\"] are…", options: ["different properties", "the same property", "a syntax error", "only equal for numbers"], answer: 1, why: "Dot and bracket notation reach the same key." },
      { q: "[1,2,3].map(n => n + 1) returns…", options: ["[1,2,3]", "[2,3,4]", "6", "undefined"], answer: 1, why: "map transforms each element into a new array." },
      { q: "filter returns…", options: ["the first match", "true/false", "a new array of passing elements", "the modified original array"], answer: 2, why: "It never mutates the original — it builds a filtered copy." },
      { q: "Which string supports ${x} interpolation?", options: ["'single'", '"double"', "`backtick`", "all of them"], answer: 2, why: "Only template literals interpolate." },
    ],
  },
};

const JS_CH3 = {
  id: "js-3", name: "Chapter 3 · Functional & Beyond", tier: "Pro",
  lessons: [
    {
      id: "js-reduce", title: "reduce", minutes: 10,
      content: [
        { t: "p", v: "reduce boils an array down to a single value — a sum, a max, an object, anything. It carries an accumulator through each element." },
        { t: "code", v: "const loot = [5, 10, 15];\nconst total = loot.reduce((acc, n) => acc + n, 0);\nconsole.log(total); // 30" },
        { t: "p", v: "The 0 is the starting accumulator. Each pass, your function returns the next accumulator value. map and filter can both be written with reduce — it's the most powerful of the trio." },
        { t: "tip", v: "Forgetting the initial value is the classic reduce bug — always pass it explicitly." },
      ],
      challenge: {
        type: "js",
        prompt: "Given const gold = [5, 10, 15], use reduce to sum it and log the total. Expected output: 30",
        starter: "const gold = [5, 10, 15];\n// reduce to riches\n",
        expectOutput: ["30"],
        hint: "console.log(gold.reduce((acc, n) => acc + n, 0));",
      },
    },
    {
      id: "js-closure", title: "Closures", minutes: 11,
      content: [
        { t: "p", v: "A closure is a function that remembers the variables from where it was created — even after that outer scope has finished. This is how JavaScript does private state." },
        { t: "code", v: "function makeCounter() {\n  let count = 0;\n  return () => ++count;\n}\nconst next = makeCounter();\nconsole.log(next()); // 1\nconsole.log(next()); // 2" },
        { t: "p", v: "count lives on inside the returned function — invisible from outside, impossible to tamper with. Interviewers love this question; now you love it too." },
        { t: "tip", v: "Every arrow function you write inside another function is quietly a closure." },
      ],
      challenge: {
        type: "js",
        prompt: "Write makeAdder(x) that returns a function taking y and returning x + y. Log makeAdder(3)(4). Expected output: 7",
        starter: "// a function factory\n",
        expectOutput: ["7"],
        hint: "function makeAdder(x) { return (y) => x + y; } console.log(makeAdder(3)(4));",
      },
    },
    {
      id: "js-class", title: "Classes", minutes: 9,
      content: [
        { t: "p", v: "JavaScript classes bundle state and behaviour: a constructor initializes each instance, and methods act on it via this." },
        { t: "code", v: 'class Player {\n  constructor(name) {\n    this.name = name;\n  }\n  ready() {\n    return `${this.name} ready`;\n  }\n}\nconsole.log(new Player("Nova").ready());' },
        { t: "p", v: "new Player(\"Nova\") creates an instance; this inside methods points at that instance. Frameworks like React grew out of exactly these patterns." },
        { t: "tip", v: "Arrow functions don't get their own this — inside class methods, stick to regular method syntax." },
      ],
      challenge: {
        type: "js",
        prompt: 'Write class Player with a constructor(name) and a ready() method returning name + " ready". Log new Player("Nova").ready(). Expected output: Nova ready',
        starter: "// class up\n",
        expectOutput: ["Nova ready"],
        hint: "class Player { constructor(name){ this.name = name; } ready(){ return this.name + \" ready\"; } }",
      },
    },
    {
      id: "js-recursion", title: "Recursion", minutes: 10,
      content: [
        { t: "p", v: "A recursive function calls itself, solving a big problem by shrinking it toward a base case. No base case = infinite loop = stack overflow." },
        { t: "code", v: "function factorial(n) {\n  if (n <= 1) return 1;        // base case\n  return n * factorial(n - 1); // shrink the problem\n}\nconsole.log(factorial(5)); // 120" },
        { t: "p", v: "Trees, nested folders, JSON structures — anything self-similar is naturally recursive. It's the pro move for traversing nested data." },
        { t: "tip", v: "Write the base case FIRST, then the recursive step. Always." },
      ],
      challenge: {
        type: "js",
        prompt: "Write a recursive factorial(n) and log factorial(5). Expected output: 120",
        starter: "// no loops allowed\n",
        expectOutput: ["120"],
        hint: "Base case: if (n <= 1) return 1. Otherwise return n * factorial(n - 1).",
      },
    },
  ],
  quiz: {
    id: "js-quiz-3",
    questions: [
      { q: "[1,2,3].reduce((a,n) => a+n, 10) equals…", options: ["6", "16", "10", "undefined"], answer: 1, why: "Starts at 10, then adds 1+2+3." },
      { q: "A closure lets a function…", options: ["run faster", "remember its creation scope", "avoid errors", "skip the call stack"], answer: 1, why: "It captures outer variables and keeps them alive." },
      { q: "Recursion MUST have…", options: ["a loop", "a base case", "an array", "a class"], answer: 1, why: "Without a base case it never stops." },
      { q: "In a class method, this refers to…", options: ["the class definition", "the window", "the instance the method was called on", "nothing"], answer: 2, why: "this is the specific object doing the work." },
    ],
  },
};

const WEB_CH2 = {
  id: "web-2", name: "Chapter 2 · Real Page Anatomy", tier: "Intermediate",
  lessons: [
    {
      id: "web-media", title: "Images & Inputs", minutes: 7,
      content: [
        { t: "p", v: "Images use the self-closing <img> tag with two key attributes: src (where the image lives) and alt (a text description for screen readers and broken links)." },
        { t: "code", v: '<img src="avatar.png" alt="Pixel avatar of a knight">\n<input type="text" placeholder="Enter your alias">\n<button>Start</button>' },
        { t: "p", v: "Inputs collect user data — type can be text, password, number, email and more. Buttons trigger actions." },
        { t: "tip", v: "alt text isn't optional decoration — it's accessibility. Describe what the image shows." },
      ],
      challenge: {
        type: "html",
        prompt: "Add an <img> with both a src and an alt attribute, plus a <button> with any label.",
        starter: "<!-- image + button -->\n",
        mustMatch: ["<img[^>]*src\\s*=\\s*[\"'][^\"']+[\"'][^>]*alt\\s*=\\s*[\"'][^\"']+[\"']|<img[^>]*alt\\s*=\\s*[\"'][^\"']+[\"'][^>]*src\\s*=\\s*[\"'][^\"']+[\"']", "<button>[\\s\\S]*?</button>"],
        hint: '<img src="pic.png" alt="a picture"> and <button>Go</button>',
      },
    },
    {
      id: "web-semantic", title: "Semantic Structure", minutes: 8,
      content: [
        { t: "p", v: "Divs everywhere is amateur hour. Semantic tags — <header>, <nav>, <main>, <section>, <footer> — describe what each region IS, helping browsers, search engines, and assistive tech." },
        { t: "code", v: "<header><h1>CodeMasters</h1></header>\n<main>\n  <section><p>Lessons live here.</p></section>\n</main>\n<footer><p>© 2026</p></footer>" },
        { t: "p", v: "Structure your page like a document: one header, one main, one footer, sections inside." },
        { t: "tip", v: "If a screen reader read your page aloud, would the structure make sense? That's the test." },
      ],
      challenge: {
        type: "html",
        prompt: "Build a page with <header>, <main>, and <footer> — each containing something.",
        starter: "<!-- semantic skeleton -->\n",
        mustMatch: ["<header>[\\s\\S]+?</header>", "<main>[\\s\\S]+?</main>", "<footer>[\\s\\S]+?</footer>"],
        hint: "<header>…</header> <main>…</main> <footer>…</footer>",
      },
    },
    {
      id: "web-classes", title: "Classes & Selectors", minutes: 9,
      content: [
        { t: "p", v: "Classes label elements so CSS can target groups of them: class=\"card\" in HTML, .card in CSS. IDs (#hero) target one unique element." },
        { t: "code", v: '<style>\n  .card { background: #1b2140; padding: 16px; border-radius: 12px; color: white; }\n</style>\n<div class="card">I am styled</div>\n<div class="card">Me too — same class</div>' },
        { t: "p", v: "One class, many elements, one rule. This is how design systems scale — you're writing your first one right now." },
        { t: "tip", v: "Class selectors start with a dot in CSS but NOT in the HTML attribute. Everyone mixes this up once." },
      ],
      challenge: {
        type: "html",
        prompt: "Create a .card class rule in a <style> tag with a background property, and use class=\"card\" on at least one element.",
        starter: "<style>\n  /* your .card rule */\n</style>\n\n",
        mustMatch: ["\\.card\\s*\\{[^}]*background[^}]*\\}", "class\\s*=\\s*[\"']card[\"']"],
        hint: '.card { background: navy; } in style, then <div class="card">hi</div>',
      },
    },
  ],
  quiz: {
    id: "web-quiz-2",
    questions: [
      { q: "The alt attribute exists for…", options: ["SEO keywords only", "accessibility & fallback text", "image speed", "styling"], answer: 1, why: "It describes the image for screen readers and when the file fails to load." },
      { q: "Which tag marks the page's primary content?", options: ["<div>", "<main>", "<body>", "<content>"], answer: 1, why: "<main> is the semantic wrapper for primary content." },
      { q: "In CSS, .card targets…", options: ["a tag named card", "every element with class=\"card\"", "one element with id=\"card\"", "nothing"], answer: 1, why: "Dot = class selector, shared by any number of elements." },
    ],
  },
};

const WEB_CH3 = {
  id: "web-3", name: "Chapter 3 · Layout Mastery", tier: "Pro",
  lessons: [
    {
      id: "web-flex", title: "Flexbox", minutes: 10,
      content: [
        { t: "p", v: "Flexbox lays out items in a row or column with effortless alignment. Set display: flex on a container and its children fall in line." },
        { t: "code", v: '<style>\n  .row { display: flex; justify-content: space-between; align-items: center; gap: 12px; }\n  .row div { background: teal; color: white; padding: 12px; }\n</style>\n<div class="row">\n  <div>One</div><div>Two</div><div>Three</div>\n</div>' },
        { t: "p", v: "justify-content controls the main axis (horizontal in a row); align-items controls the cross axis. gap adds space between children — no margin hacks." },
        { t: "tip", v: "Centering anything: display:flex; justify-content:center; align-items:center. The ancient riddle, solved." },
      ],
      challenge: {
        type: "html",
        prompt: "Create a container rule with display: flex and justify-content, and give it at least two child elements.",
        starter: "<style>\n  /* flex it */\n</style>\n\n",
        mustMatch: ["display\\s*:\\s*flex", "justify-content\\s*:"],
        hint: ".row { display: flex; justify-content: space-between; } then a div with two children.",
      },
    },
    {
      id: "web-grid", title: "CSS Grid", minutes: 10,
      content: [
        { t: "p", v: "Grid handles two-dimensional layout — rows AND columns at once. Define the column structure on the container and children fill the cells." },
        { t: "code", v: '<style>\n  .grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }\n  .grid div { background: purple; color: white; padding: 14px; }\n</style>\n<div class="grid">\n  <div>1</div><div>2</div><div>3</div>\n  <div>4</div><div>5</div><div>6</div>\n</div>' },
        { t: "p", v: "1fr means one fraction of available space — three 1fr columns split the width evenly. repeat(3, 1fr) is the shorthand." },
        { t: "tip", v: "Rule of thumb: flexbox for one direction (navbars, toolbars), grid for full layouts (galleries, dashboards)." },
      ],
      challenge: {
        type: "html",
        prompt: "Create a rule with display: grid and grid-template-columns using fr units, plus a container with several children.",
        starter: "<style>\n  /* grid it */\n</style>\n\n",
        mustMatch: ["display\\s*:\\s*grid", "grid-template-columns\\s*:[^;]*fr"],
        hint: ".grid { display: grid; grid-template-columns: 1fr 1fr; }",
      },
    },
    {
      id: "web-responsive", title: "Responsive Design", minutes: 11,
      content: [
        { t: "p", v: "Screens range from watches to cinema displays. Media queries apply CSS conditionally by viewport size — the core of responsive design." },
        { t: "code", v: "<style>\n  .panel { background: steelblue; color: white; padding: 20px; }\n  @media (max-width: 600px) {\n    .panel { background: crimson; }\n  }\n</style>\n<div class=\"panel\">Resize me (narrow preview = crimson)</div>" },
        { t: "p", v: "max-width: 600px means 'at 600px or narrower'. Mobile-first pros write base styles for small screens, then min-width queries for bigger ones." },
        { t: "tip", v: "Test the breakpoints where your layout actually breaks, not just standard device sizes." },
      ],
      challenge: {
        type: "html",
        prompt: "Write a @media (max-width: ...) query that changes any property, plus the element it styles.",
        starter: "<style>\n  /* base + breakpoint */\n</style>\n\n",
        mustMatch: ["@media\\s*\\(\\s*max-width\\s*:", "\\{[^}]*:[^}]*\\}"],
        hint: "@media (max-width: 600px) { .panel { background: red; } }",
      },
    },
  ],
  quiz: {
    id: "web-quiz-3",
    questions: [
      { q: "In a flex ROW, justify-content aligns items…", options: ["vertically", "horizontally", "diagonally", "not at all"], answer: 1, why: "It works along the main axis — horizontal in a row." },
      { q: "grid-template-columns: 1fr 2fr creates…", options: ["two equal columns", "a column twice as wide as the other", "two rows", "an error"], answer: 1, why: "fr units share space proportionally — 1:2." },
      { q: "@media (max-width: 600px) applies when the viewport is…", options: ["exactly 600px", "600px or narrower", "wider than 600px", "printing"], answer: 1, why: "max-width = 'up to this size'." },
      { q: "Best tool for a 2-D photo gallery layout?", options: ["floats", "flexbox", "grid", "tables"], answer: 2, why: "Grid is built for rows-and-columns layouts." },
    ],
  },
};

const SQL_CH2 = {
  id: "sql-2", name: "Chapter 2 · Changing Data", tier: "Intermediate",
  lessons: [
    {
      id: "sql-insert", title: "INSERT", minutes: 7,
      content: [
        { t: "p", v: "INSERT INTO adds new rows. Name the table, list the columns, then supply matching VALUES." },
        { t: "code", v: "INSERT INTO agents (name, clearance)\nVALUES ('Nova', 5);" },
        { t: "p", v: "Column order and value order must match. Strings in single quotes, numbers bare." },
        { t: "tip", v: "You can insert multiple rows at once: VALUES ('a', 1), ('b', 2);" },
      ],
      challenge: {
        type: "pattern",
        prompt: "Insert a row into agents with columns (name, clearance) and values ('Nova', 5).",
        starter: "-- new recruit\n",
        mustMatch: ["insert\\s+into\\s+agents\\s*\\(\\s*name\\s*,\\s*clearance\\s*\\)", "values\\s*\\(\\s*'Nova'\\s*,\\s*5\\s*\\)"],
        flags: "i",
        hint: "INSERT INTO agents (name, clearance) VALUES ('Nova', 5);",
      },
    },
    {
      id: "sql-update", title: "UPDATE & DELETE", minutes: 8,
      content: [
        { t: "p", v: "UPDATE changes existing rows; DELETE removes them. BOTH obey the WHERE clause — and both are catastrophic without it." },
        { t: "code", v: "UPDATE agents SET clearance = 3 WHERE name = 'Nova';\nDELETE FROM agents WHERE retired = 1;" },
        { t: "p", v: "Forget the WHERE on an UPDATE and you just changed EVERY row. Forget it on DELETE and the table is empty. Every DBA has a scar story." },
        { t: "tip", v: "Pro habit: write the WHERE clause first, or run a SELECT with the same WHERE to preview what you're about to hit." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Update agents: set clearance = 1 where name = 'Grub'.",
        starter: "-- demote the goblin\n",
        mustMatch: ["update\\s+agents\\s+set\\s+clearance\\s*=\\s*1\\s+where\\s+name\\s*=\\s*'Grub'"],
        flags: "i",
        hint: "UPDATE agents SET clearance = 1 WHERE name = 'Grub';",
      },
    },
    {
      id: "sql-agg", title: "Aggregates & GROUP BY", minutes: 9,
      content: [
        { t: "p", v: "Aggregate functions summarize many rows into one number: COUNT(*), AVG(col), SUM(col), MAX(col), MIN(col)." },
        { t: "code", v: "SELECT team, COUNT(*) AS members, AVG(xp) AS avg_xp\nFROM players\nGROUP BY team;" },
        { t: "p", v: "GROUP BY splits rows into buckets (one per team here) and aggregates within each bucket. AS renames output columns." },
        { t: "tip", v: "Every non-aggregated column in the SELECT must appear in GROUP BY — the database will remind you loudly." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Count players per team: select team and COUNT(*) from players, grouped by team.",
        starter: "-- headcount\n",
        mustMatch: ["select\\s+team\\s*,\\s*count\\s*\\(\\s*\\*\\s*\\)", "from\\s+players", "group\\s+by\\s+team"],
        flags: "i",
        hint: "SELECT team, COUNT(*) FROM players GROUP BY team;",
      },
    },
  ],
  quiz: {
    id: "sql-quiz-2",
    questions: [
      { q: "DELETE FROM users; (no WHERE) does what?", options: ["Deletes one row", "Deletes nothing", "Deletes every row", "Syntax error"], answer: 2, why: "No WHERE means the whole table. Handle with fear." },
      { q: "Which counts rows per category?", options: ["ORDER BY", "GROUP BY with COUNT(*)", "LIMIT", "DISTINCT"], answer: 1, why: "GROUP BY buckets rows; COUNT(*) counts each bucket." },
      { q: "AVG(xp) returns…", options: ["the highest xp", "total xp", "the mean xp", "row count"], answer: 2, why: "AVG is the arithmetic mean." },
    ],
  },
};

const SQL_CH3 = {
  id: "sql-3", name: "Chapter 3 · Joins & Safety", tier: "Pro",
  lessons: [
    {
      id: "sql-join", title: "JOINs", minutes: 11,
      content: [
        { t: "p", v: "Real databases split data across tables — players in one, teams in another, linked by an id. JOIN stitches them back together." },
        { t: "code", v: "SELECT players.name, teams.name AS team\nFROM players\nJOIN teams ON players.team_id = teams.id;" },
        { t: "p", v: "The ON clause says how rows match. INNER JOIN (the default) keeps only matching rows; LEFT JOIN keeps every left row even without a match." },
        { t: "tip", v: "table.column syntax disambiguates when both tables have a 'name' column — which they always do." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Join players to teams: select players.name from players joined to teams on players.team_id = teams.id.",
        starter: "-- stitch the tables\n",
        mustMatch: ["from\\s+players", "join\\s+teams", "on\\s+players\\.team_id\\s*=\\s*teams\\.id"],
        flags: "i",
        hint: "SELECT players.name FROM players JOIN teams ON players.team_id = teams.id;",
      },
    },
    {
      id: "sql-sub", title: "Subqueries", minutes: 10,
      content: [
        { t: "p", v: "A subquery is a query inside a query — compute a value on the fly and compare against it." },
        { t: "code", v: "SELECT name, xp FROM players\nWHERE xp > (SELECT AVG(xp) FROM players);\n-- everyone above average" },
        { t: "p", v: "The inner query runs first, produces AVG(xp), and the outer WHERE uses it. Subqueries also work in FROM and SELECT clauses." },
        { t: "tip", v: "If a subquery gets hairy, a JOIN or a WITH clause (CTE) is usually the cleaner pro move." },
      ],
      challenge: {
        type: "pattern",
        prompt: "Select name from players where xp is greater than the average xp (use a subquery with AVG).",
        starter: "-- above the curve\n",
        mustMatch: ["where\\s+xp\\s*>\\s*\\(\\s*select\\s+avg\\s*\\(\\s*xp\\s*\\)\\s+from\\s+players\\s*\\)"],
        flags: "i",
        hint: "… WHERE xp > (SELECT AVG(xp) FROM players);",
      },
    },
    {
      id: "sql-inject", title: "SQL Injection (Defense)", minutes: 10,
      content: [
        { t: "p", v: "If an app glues user input directly into a query string, an attacker can type SQL instead of data and rewrite the query's meaning. This is SQL injection — decades old and still breaching companies." },
        { t: "code", v: "-- App builds: SELECT * FROM users WHERE name = '<input>'\n-- Attacker types:  ' OR '1'='1\n-- Query becomes:   … WHERE name = '' OR '1'='1'   → matches EVERY row\n\n-- THE FIX — parameterized queries:\n-- query(\"SELECT * FROM users WHERE name = ?\", [input])" },
        { t: "p", v: "Parameterized (prepared) statements send the query structure and the data separately — input can never become SQL, no matter what it contains. Combine with least-privilege database accounts and input validation for defense in depth." },
        { t: "tip", v: "This is why the Cybersecurity course and SQL course share a border. Crossing it makes you dangerous — the good kind." },
      ],
      challenge: {
        type: "text",
        prompt: "What is the standard defense that separates query structure from user data? (two words)",
        answer: ["parameterized queries", "parameterised queries", "prepared statements", "parameterized statements", "parameterized query", "prepared statement"],
        hint: "Also called prepared statements — the ? placeholder technique.",
      },
    },
  ],
  quiz: {
    id: "sql-quiz-3",
    questions: [
      { q: "JOIN … ON defines…", options: ["sort order", "how rows from two tables match", "column names", "row limits"], answer: 1, why: "ON is the matching condition between tables." },
      { q: "An INNER JOIN keeps…", options: ["all rows from both tables", "only rows with a match in both", "only left-table rows", "duplicates only"], answer: 1, why: "No match, no row — that's INNER." },
      { q: "A subquery in WHERE runs…", options: ["after the outer query", "before/for the outer comparison", "never", "only with GROUP BY"], answer: 1, why: "Its result feeds the outer condition." },
      { q: "The real fix for SQL injection is…", options: ["hiding error messages", "parameterized queries", "longer passwords", "a firewall"], answer: 1, why: "Separate structure from data — input can never become code." },
    ],
  },
};

const CY_CH2 = {
  id: "cy-2", name: "Chapter 2 · Networks & Defenses", tier: "Intermediate",
  lessons: [
    {
      id: "cy-net", title: "IPs, Ports & DNS", minutes: 9,
      content: [
        { t: "p", v: "Every device on a network has an IP address (like 192.168.1.10). Ports are numbered doors on that address — each service listens on its own: web traffic on 80 (HTTP) and 443 (HTTPS), SSH on 22, DNS on 53." },
        { t: "code", v: "you type:  techaway.io\nDNS asks:  what's the IP for techaway.io?  → 203.0.113.7\nbrowser:   connects to 203.0.113.7, port 443" },
        { t: "p", v: "DNS is the internet's phonebook, translating names to IPs. Security teams watch DNS closely — malware phoning home has to look something up too." },
        { t: "tip", v: "Private ranges (192.168.x.x, 10.x.x.x) live inside local networks and aren't reachable from the internet." },
      ],
      challenge: {
        type: "text",
        prompt: "Which port does HTTPS use by default? (a number)",
        answer: ["443", "port 443"],
        hint: "HTTP is 80; the secure version is 443.",
      },
    },
    {
      id: "cy-tls", title: "HTTPS & Encryption in Transit", minutes: 9,
      content: [
        { t: "p", v: "Plain HTTP travels as readable text — anyone on the network path (café Wi-Fi included) can read or alter it. HTTPS wraps the connection in TLS encryption." },
        { t: "p", v: "TLS gives you three things: confidentiality (eavesdroppers see gibberish), integrity (tampering is detected), and authentication (a certificate proves you reached the real site, not an impostor)." },
        { t: "code", v: "HTTP   →  'password=hunter2'      visible to the network\nHTTPS  →  '8f3aQ...x92='           gibberish without the keys" },
        { t: "tip", v: "The padlock means the CONNECTION is encrypted — not that the site is trustworthy. Phishing sites use HTTPS too." },
      ],
      challenge: {
        type: "text",
        prompt: "What protocol does HTTPS use to encrypt traffic? (three letters — the modern successor to SSL)",
        answer: ["tls"],
        hint: "Transport Layer Security.",
      },
    },
    {
      id: "cy-malware", title: "Malware Taxonomy", minutes: 8,
      content: [
        { t: "p", v: "Know your enemy's species: a virus attaches to files and spreads when they run; a worm spreads by itself across networks; a trojan disguises itself as legitimate software; ransomware encrypts data and demands payment; spyware quietly watches." },
        { t: "code", v: "virus      → needs a host file + human action\nworm       → self-spreading, no clicks needed\ntrojan     → 'free game.exe' with a surprise inside\nransomware → your files, encrypted, held hostage\nspyware    → keylogging, screen capture, exfiltration" },
        { t: "p", v: "Defenses layer up: updated software closes the holes worms use, backups defang ransomware, and healthy suspicion of downloads starves trojans." },
        { t: "tip", v: "'Antivirus' is a misnomer now — modern endpoint protection hunts behaviour, not just known file signatures." },
      ],
      challenge: {
        type: "text",
        prompt: "Which malware type disguises itself as legitimate software to trick you into running it? (one word)",
        answer: ["trojan", "a trojan", "trojan horse"],
        hint: "Named after a certain wooden horse full of Greeks.",
      },
    },
    {
      id: "cy-defense", title: "Defense in Depth", minutes: 9,
      content: [
        { t: "p", v: "No single control is enough — pros stack layers so that when one fails, others hold. Firewalls filter network traffic, updates patch known holes, MFA survives stolen passwords, backups survive ransomware." },
        { t: "p", v: "The principle of least privilege ties it together: every account and program gets the MINIMUM access it needs. An intern's account shouldn't be able to delete the production database — then a hacked intern account can't either." },
        { t: "code", v: "attacker gets past →  firewall\n         blocked by →  patched software\n  or slowed down by →  least privilege\n     or survived by →  backups + monitoring" },
        { t: "tip", v: "Assume breach: design as if attackers WILL get in, and limit what they can do once inside." },
      ],
      challenge: {
        type: "text",
        prompt: "What principle says every account gets only the minimum access it needs? (two words)",
        answer: ["least privilege", "principle of least privilege"],
        hint: "Least ________. The intern can't drop the database.",
      },
    },
  ],
  quiz: {
    id: "cy-quiz-2",
    questions: [
      { q: "DNS translates…", options: ["IPs to MAC addresses", "domain names to IPs", "ports to services", "HTTP to HTTPS"], answer: 1, why: "It's the internet's phonebook." },
      { q: "The HTTPS padlock guarantees…", options: ["the site is honest", "the connection is encrypted", "no malware on the page", "your data is deleted"], answer: 1, why: "Encrypted transit — trustworthiness of the site is a separate question." },
      { q: "Malware that spreads across networks by itself is a…", options: ["virus", "worm", "trojan", "rootkit"], answer: 1, why: "Worms self-propagate; viruses need a host and a human." },
      { q: "Defense in depth means…", options: ["one very strong firewall", "multiple layered controls", "hiding the server", "encrypting twice"], answer: 1, why: "Layers, so one failure isn't game over." },
    ],
  },
};

const CY_CH3 = {
  id: "cy-3", name: "Chapter 3 · The Defender's Playbook", tier: "Pro",
  lessons: [
    {
      id: "cy-webvuln", title: "Web Vulnerabilities (OWASP)", minutes: 11,
      content: [
        { t: "p", v: "The OWASP Top 10 catalogs the web's most common vulnerability classes. Two royalty-tier examples: injection (untrusted input becoming code — you met SQL injection in the SQL course) and XSS, cross-site scripting." },
        { t: "p", v: "XSS happens when a site displays user input without neutralizing it, letting attacker-supplied script run in other visitors' browsers — stealing sessions or defacing pages. The defense is output encoding: user input gets rendered as inert text, never executed." },
        { t: "code", v: "vulnerable:  page shows raw input   → input can run as script\ndefended:    page ESCAPES input     → <script> becomes visible text\n\ndefender's checklist:\n  validate input · encode output · parameterize queries\n  set a Content-Security-Policy · least-privilege everything" },
        { t: "tip", v: "One mindset covers most of the Top 10: never let data cross a trust boundary and get treated as code." },
      ],
      challenge: {
        type: "text",
        prompt: "Untrusted input executing as script in a victim's browser is called…? (three letters)",
        answer: ["xss", "x.s.s"],
        hint: "Cross-Site Scripting, abbreviated.",
      },
    },
    {
      id: "cy-crypto2", title: "Modern Cryptography", minutes: 11,
      content: [
        { t: "p", v: "Symmetric encryption (like AES) uses ONE shared key — fast, great for bulk data, but both sides must somehow share the key safely first. Asymmetric encryption (like RSA) solves that with a key PAIR: a public key anyone can have, and a private key that never leaves you." },
        { t: "code", v: "symmetric:   same key locks and unlocks   (AES)\nasymmetric:  public key locks →  private key unlocks (RSA)\n\nreal TLS:    asymmetric handshake exchanges a fresh\n             symmetric key → bulk traffic uses AES" },
        { t: "p", v: "Digital signatures flip the pair: sign with your private key, and anyone can verify with your public key — proving the message came from you, unmodified. Salted, slow hashes (bcrypt, argon2) protect stored passwords." },
        { t: "tip", v: "Rule one of crypto club: never invent your own. Use vetted libraries and algorithms." },
      ],
      challenge: {
        type: "text",
        prompt: "In asymmetric cryptography, which key can you share openly with the whole world? (one or two words)",
        answer: ["public", "public key", "the public key"],
        hint: "Its partner — the private key — never leaves your machine.",
      },
    },
    {
      id: "cy-ir", title: "Incident Response", minutes: 10,
      content: [
        { t: "p", v: "Breaches happen even to great teams — what separates pros is the response. The classic lifecycle: Prepare → Identify → Contain → Eradicate → Recover → Lessons Learned." },
        { t: "code", v: "IDENTIFY   alerts fire, logs confirm — what happened, where?\nCONTAIN    isolate infected hosts, revoke credentials — stop the spread\nERADICATE  remove the malware / close the hole they used\nRECOVER    restore from clean backups, watch for re-entry\nLESSONS    blameless post-mortem → fix the process, not the person" },
        { t: "p", v: "Containment is the pressure moment: seconds matter, and pulling the right cable beats pulling all of them. This is also where log analysis pays off — you'll practice exactly that in the Log Hunter lab." },
        { t: "tip", v: "Blameless post-mortems are the pro culture marker. Punish honesty and people start hiding incidents." },
      ],
      challenge: {
        type: "text",
        prompt: "After identifying a breach, which phase stops it from spreading? (one word)",
        answer: ["containment", "contain", "contain it"],
        hint: "Isolate the hosts, cut the access — ________ the damage.",
      },
    },
  ],
  quiz: {
    id: "cy-quiz-3",
    questions: [
      { q: "The core defense against XSS is…", options: ["a strong password", "output encoding / escaping", "a VPN", "port blocking"], answer: 1, why: "Escaped input renders as text, never as script." },
      { q: "AES is ______ encryption; RSA is ______.", options: ["asymmetric / symmetric", "symmetric / asymmetric", "both symmetric", "both asymmetric"], answer: 1, why: "AES = one shared key; RSA = public/private pair." },
      { q: "You sign a message with your…", options: ["public key", "private key", "session cookie", "password hash"], answer: 1, why: "Sign private, verify public." },
      { q: "The right order after identifying an incident:", options: ["recover → contain", "contain → eradicate → recover", "eradicate → identify", "lessons → contain"], answer: 1, why: "Stop the spread, remove the cause, then rebuild." },
      { q: "Passwords should be stored as…", options: ["plaintext", "Base64", "reversible encryption", "salted slow hashes"], answer: 3, why: "bcrypt/argon2 with salts — slow to crack, unique per user." },
    ],
  },
};

/* wire the new chapters in (runs before allLessons is computed) */
COURSES.find((c) => c.id === "python").chapters.push(PY_CH2, PY_CH3);
COURSES.find((c) => c.id === "javascript").chapters.push(JS_CH2, JS_CH3);
COURSES.find((c) => c.id === "htmlcss").chapters.push(WEB_CH2, WEB_CH3);
COURSES.find((c) => c.id === "sql").chapters.push(SQL_CH2, SQL_CH3);
COURSES.find((c) => c.id === "cyber").chapters.push(CY_CH2, CY_CH3);
COURSES.forEach((c) => c.chapters.forEach((ch, i) => { if (!ch.tier) ch.tier = ["Beginner", "Intermediate", "Pro"][i] || "Pro"; }));

/* ===== FINAL PROJECTS (AI-graded capstones → certificates) ===== */
const PROJECTS = {
  python: {
    id: "proj-python", xp: 150,
    brief: "Build a text-based inventory manager. Requirements: a list or dict holding items; functions to add and remove an item; a loop that would process at least 3 operations; sensible prints showing state. Paste your full program.",
    placeholder: "# Inventory Manager\n# functions + data structure + loop\n",
  },
  javascript: {
    id: "proj-javascript", xp: 150,
    brief: "Build a mini stats engine. Requirements: an array of at least 3 player objects (name, xp); use map, filter AND reduce at least once each (e.g., names list, players above a threshold, total xp); console.log the results. Paste your full program.",
    placeholder: "// Stats engine: map + filter + reduce\nconst players = [\n];\n",
  },
  htmlcss: {
    id: "proj-htmlcss", xp: 150,
    brief: "Build a profile card page. Requirements: semantic structure (header/main/footer); a styled card using a class; flexbox OR grid somewhere; at least one custom color and border-radius. Paste the full HTML including the <style> tag.",
    placeholder: "<style>\n  /* your design system */\n</style>\n<header>\n</header>\n",
  },
  sql: {
    id: "proj-sql", xp: 150,
    brief: "Given tables players(id, name, xp, team_id) and teams(id, name): write THREE queries — (1) top 5 players by xp, (2) average xp per team name using a JOIN and GROUP BY, (3) players above the overall average xp using a subquery. Paste all three.",
    placeholder: "-- Query 1: top 5\n\n-- Query 2: avg per team (JOIN + GROUP BY)\n\n-- Query 3: above-average players (subquery)\n",
  },
  cyber: {
    id: "proj-cyber", xp: 150,
    brief: "Security assessment. Scenario: a small startup runs a customer web app; passwords are stored in plaintext, everyone shares one admin account, the server hasn't been updated in a year, and staff get frequent 'reset your password' emails. Write a short assessment: identify at least 3 distinct risks and give a concrete fix for each, using the concepts from this course.",
    placeholder: "RISK 1: ...\nFIX 1: ...\n\nRISK 2: ...\nFIX 2: ...\n\nRISK 3: ...\nFIX 3: ...\n",
  },
};

/* ===== NEW PRO LAB: LOG HUNTER ===== */
LABS.push({
  id: "lab-logs",
  name: "Log Hunter",
  icon: "🔎",
  tint: T.violet,
  difficulty: "Pro",
  kind: "questions",
  desc: "A server was breached overnight. You have the auth log. Trace the attack, answer the analyst questions, earn the flag.",
  tasks: [
    "Read the log top to bottom — timestamps tell a story",
    "Repeated failures from one IP is a pattern with a name",
    "Find the moment failure turns into success",
    "Answer all three questions correctly to reveal the flag",
  ],
  log: `Jun 30 02:14:01 srv01 sshd[8811]: Failed password for admin from 203.0.113.66 port 51022
Jun 30 02:14:03 srv01 sshd[8811]: Failed password for admin from 203.0.113.66 port 51023
Jun 30 02:14:05 srv01 sshd[8811]: Failed password for admin from 203.0.113.66 port 51024
Jun 30 02:14:08 srv01 sshd[8811]: Failed password for admin from 203.0.113.66 port 51025
Jun 30 02:14:11 srv01 sshd[8811]: Failed password for admin from 203.0.113.66 port 51026
Jun 30 02:14:14 srv01 sshd[8812]: Failed password for root from 203.0.113.66 port 51027
Jun 30 02:14:20 srv01 sshd[8813]: Accepted password for admin from 203.0.113.66 port 51028
Jun 30 02:14:31 srv01 sudo:    admin : TTY=pts/0 ; COMMAND=/bin/cat /etc/shadow
Jun 30 02:15:02 srv01 sshd[8820]: Accepted publickey for deploy from 10.0.0.12 port 40110
Jun 30 08:00:14 srv01 sshd[9101]: Accepted publickey for alice from 10.0.0.31 port 40551`,
  questions: [
    { id: "lq-1", q: "What is the attacker's IP address?", answer: ["203.0.113.66"], hint: "The IP hammering the door at 02:14." },
    { id: "lq-2", q: "Which account did the attacker successfully log into?", answer: ["admin"], hint: "Look for 'Accepted password' from the attacker's IP." },
    { id: "lq-3", q: "Many rapid failed logins guessing passwords is called a ______ attack. (two words or one)", answer: ["brute force", "brute-force", "bruteforce", "brute force attack", "password brute force"], hint: "Trying every key on the ring, fast." },
  ],
  flags: [{ id: "log-flag", label: "Analyst verdict", value: "CodeMasters{gr3p_th3_l0gs}", xp: 70 }],
});

BADGES.push(
  { id: "graduate", icon: "🎓", name: "Graduate", desc: "Pass your first final project" },
  { id: "grandmaster", icon: "👑", name: "Grandmaster", desc: "Earn every course certificate" },
  { id: "interview-ace", icon: "🎤", name: "Interview Ace", desc: "Crush a mock interview (or run 3)" },
);


/* ===================== JAVA COURSE ===================== */

const JAVA_COURSE = {
  id: "java",
  name: "Java",
  icon: "☕",
  tint: "#FF8A3D",
  tagline: "Strict, typed, and everywhere — from Android to banks",
  desc: "The industry workhorse. Learn static typing and true object-oriented design.",
  chapters: [
    {
      id: "java-1", name: "Chapter 1 · Hello, JVM", tier: "Beginner",
      lessons: [
        {
          id: "java-hello", title: "Hello, World! (Java Edition)", minutes: 6,
          content: [
            { t: "p", v: "Java is stricter than Python or JavaScript: all code lives inside classes, and programs start from a main method. It looks ceremonial at first — that ceremony is what keeps million-line codebases sane." },
            { t: "code", v: 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello, World!");\n    }\n}' },
            { t: "p", v: "System.out.println() prints a line. Every statement ends with a semicolon — Java does not forgive forgetting it. Code blocks live inside curly braces." },
            { t: "tip", v: "Your code compiles to bytecode and runs on the JVM (Java Virtual Machine) — the same program runs on Windows, Mac, Linux, and 3 billion other devices, allegedly." },
          ],
          challenge: {
            type: "pattern",
            prompt: 'Write the print statement (just the line, no class needed) that outputs: Hello, CodeMasters!',
            starter: "// one statement, semicolon included\n",
            mustMatch: ["System\\.out\\.println\\s*\\(\\s*\"Hello, CodeMasters!\"\\s*\\)\\s*;"],
            hint: 'System.out.println("Hello, CodeMasters!");',
          },
        },
        {
          id: "java-vars", title: "Typed Variables", minutes: 8,
          content: [
            { t: "p", v: "Java is statically typed: every variable declares its type up front, and the compiler enforces it forever. int for whole numbers, double for decimals, boolean for true/false, String for text." },
            { t: "code", v: 'int score = 9001;\ndouble accuracy = 99.5;\nboolean alive = true;\nString name = "Nova";\nSystem.out.println(score);' },
            { t: "p", v: 'Try score = "hello" and the compiler refuses to even build the program. Entire categories of bugs die at compile time instead of exploding in production — this is why banks love Java.' },
            { t: "tip", v: "String is capitalized because it's a class, not a primitive. The eight primitives (int, double, boolean, char…) are lowercase." },
          ],
          challenge: {
            type: "pattern",
            prompt: "Declare an int called power set to 9001, then print it.",
            starter: "// declare with a type\n",
            mustMatch: ["int\\s+power\\s*=\\s*9001\\s*;", "System\\.out\\.println\\s*\\(\\s*power\\s*\\)"],
            hint: "int power = 9001; then System.out.println(power);",
          },
        },
        {
          id: "java-if", title: "Conditionals", minutes: 7,
          content: [
            { t: "p", v: "Java's if uses parentheses around the condition and braces around the body — no indentation rules, the braces ARE the block." },
            { t: "code", v: 'int hp = 20;\nif (hp <= 0) {\n    System.out.println("Game over");\n} else {\n    System.out.println("Keep fighting!");\n}' },
            { t: "p", v: "Conditions must be actual booleans — if (1) is a compile error in Java, unlike in JavaScript. The compiler keeps you honest." },
            { t: "tip", v: "Always write the braces, even for one-line bodies. Apple's infamous 'goto fail' security bug came from a brace-less if." },
          ],
          challenge: {
            type: "pattern",
            prompt: 'Write an if: when score > 100, print "Legendary". Braces required.',
            starter: "int score = 150;\n// guard it with braces\n",
            mustMatch: ["if\\s*\\(\\s*score\\s*>\\s*100\\s*\\)\\s*\\{", "System\\.out\\.println\\s*\\(\\s*\"Legendary\"\\s*\\)\\s*;"],
            hint: 'if (score > 100) { System.out.println("Legendary"); }',
          },
        },
        {
          id: "java-loops", title: "The Classic for Loop", minutes: 8,
          content: [
            { t: "p", v: "Java's classic for loop has three parts: initialize a counter; the condition to keep going; the step. It's the loop every C-family language shares." },
            { t: "code", v: 'for (int i = 0; i < 3; i++) {\n    System.out.println("Respawn " + i);\n}\n// Respawn 0, Respawn 1, Respawn 2' },
            { t: "p", v: "i++ means i = i + 1. The + operator also glues strings together, converting numbers automatically." },
            { t: "tip", v: "i < 3 runs 3 times (0,1,2). i <= 3 runs 4 times. Off-by-one errors are a rite of passage." },
          ],
          challenge: {
            type: "pattern",
            prompt: 'Write a for loop with an int counter that prints "pwned" 5 times (i = 0; i < 5; i++).',
            starter: "// the classic three-parter\n",
            mustMatch: ["for\\s*\\(\\s*int\\s+i\\s*=\\s*0\\s*;\\s*i\\s*<\\s*5\\s*;\\s*i\\+\\+\\s*\\)", "System\\.out\\.println\\s*\\(\\s*\"pwned\"\\s*\\)"],
            hint: 'for (int i = 0; i < 5; i++) { System.out.println("pwned"); }',
          },
        },
      ],
      quiz: {
        id: "java-quiz-1",
        questions: [
          { q: "Every Java program starts executing from…", options: ["the first line of the file", "public static void main", "the constructor", "init()"], answer: 1, why: "main(String[] args) is the JVM's entry point." },
          { q: "Which declaration is valid Java?", options: ["x = 5", "int x = 5;", "let x = 5;", "var x := 5"], answer: 1, why: "Type first, semicolon last — that's Java." },
          { q: "int hp = \"full\"; will…", options: ["set hp to 0", "work fine", "fail at compile time", "fail only when run"], answer: 2, why: "Static typing: the compiler rejects mismatched types before the program exists." },
          { q: "for (int i = 0; i < 4; i++) runs how many times?", options: ["3", "4", "5", "infinite"], answer: 1, why: "i takes 0,1,2,3 — four passes." },
        ],
      },
    },
    {
      id: "java-2", name: "Chapter 2 · Methods & Collections", tier: "Intermediate",
      lessons: [
        {
          id: "java-methods", title: "Methods & Return Types", minutes: 9,
          content: [
            { t: "p", v: "Java methods declare their return type up front — int square(...) MUST return an int, and void means it returns nothing. Parameters are typed too." },
            { t: "code", v: "static int square(int n) {\n    return n * n;\n}\n\nSystem.out.println(square(12)); // 144" },
            { t: "p", v: "static means the method belongs to the class itself rather than an object instance — fine for utility functions, and required to be callable from main without creating objects." },
            { t: "tip", v: "The compiler checks every path returns the right type. A branch that forgets to return won't compile." },
          ],
          challenge: {
            type: "pattern",
            prompt: "Write a static method: static int square(int n) that returns n * n.",
            starter: "// typed in, typed out\n",
            mustMatch: ["static\\s+int\\s+square\\s*\\(\\s*int\\s+n\\s*\\)", "return\\s+n\\s*\\*\\s*n\\s*;"],
            hint: "static int square(int n) { return n * n; }",
          },
        },
        {
          id: "java-arrays", title: "Arrays", minutes: 8,
          content: [
            { t: "p", v: "Java arrays have a fixed length and a fixed element type, declared with square brackets on the type." },
            { t: "code", v: 'String[] gear = {"rope", "torch", "map"};\nSystem.out.println(gear[0]);      // rope\nSystem.out.println(gear.length);  // 3' },
            { t: "p", v: "Index past the end and you get an ArrayIndexOutOfBoundsException at runtime — loud, clear, and pointing at the exact line. Compare that to silent undefined in JavaScript." },
            { t: "tip", v: "Array length is .length (no parentheses); String length is .length() (a method). Java keeps you on your toes." },
          ],
          challenge: {
            type: "pattern",
            prompt: "Declare a String[] called gear with at least 3 items, then print the first element.",
            starter: "// fixed-size loadout\n",
            mustMatch: ["String\\[\\]\\s+gear\\s*=\\s*\\{[^}]+,[^}]+,[^}]+\\}", "System\\.out\\.println\\s*\\(\\s*gear\\[0\\]\\s*\\)"],
            hint: 'String[] gear = {"a", "b", "c"}; then System.out.println(gear[0]);',
          },
        },
        {
          id: "java-strings", title: "Strings & .equals()", minutes: 9,
          content: [
            { t: "p", v: "The most famous Java gotcha: == compares whether two Strings are the SAME OBJECT in memory; .equals() compares their CONTENTS. You almost always want .equals()." },
            { t: "code", v: 'String name = "Ada";\nif (name.equals("Ada")) {\n    System.out.println("Access granted");\n}\n\nname.length();        // 3\nname.toUpperCase();   // "ADA"' },
            { t: "p", v: "Strings are immutable — toUpperCase() returns a NEW string, the original is untouched. Method calls chain nicely: name.trim().toLowerCase()." },
            { t: "tip", v: 'Comparing strings with == sometimes "works" by coincidence (string pooling) and then betrays you in production. .equals(). Always.' },
          ],
          challenge: {
            type: "pattern",
            prompt: 'Write an if that checks name against "Ada" the CORRECT way (contents, not identity) and prints anything.',
            starter: 'String name = "Ada";\n// not == …\n',
            mustMatch: ["if\\s*\\(\\s*name\\.equals\\s*\\(\\s*\"Ada\"\\s*\\)\\s*\\)", "System\\.out\\.println\\s*\\("],
            hint: 'if (name.equals("Ada")) { System.out.println("hi"); }',
          },
        },
      ],
      quiz: {
        id: "java-quiz-2",
        questions: [
          { q: "A method declared void…", options: ["returns 0", "returns null", "returns nothing", "can't have parameters"], answer: 2, why: "void = no return value at all." },
          { q: "To compare String CONTENTS you use…", options: ["==", ".equals()", "===", ".compare"], answer: 1, why: "== checks object identity; .equals() checks the characters." },
          { q: "String[] a = {\"x\",\"y\"}; a.length is…", options: ["a method call", "2", "an error", "\"xy\""], answer: 1, why: "Array length is a field (no parens) — here 2." },
          { q: "Java arrays…", options: ["grow automatically", "have fixed length and type", "hold mixed types", "start at index 1"], answer: 1, why: "Fixed size, one type, zero-indexed." },
        ],
      },
    },
    {
      id: "java-3", name: "Chapter 3 · True Object-Orientation", tier: "Pro",
      lessons: [
        {
          id: "java-class", title: "Classes & Encapsulation", minutes: 11,
          content: [
            { t: "p", v: "Java is object-oriented to the bone. A class declares typed fields, a constructor (same name as the class) initializes them, and methods define behaviour. private fields + public methods = encapsulation: outsiders interact through your API, never your internals." },
            { t: "code", v: "public class Enemy {\n    private int hp;\n    private String name;\n\n    public Enemy(String name) {\n        this.name = name;\n        this.hp = 50;\n    }\n\n    public void hit(int dmg) {\n        this.hp -= dmg;\n    }\n}" },
            { t: "p", v: "this.hp distinguishes the field from a parameter with the same name. new Enemy(\"Grub\") calls the constructor and returns a fresh instance." },
            { t: "tip", v: "Make fields private by default. Every field you expose publicly is a promise you must keep forever." },
          ],
          challenge: {
            type: "pattern",
            prompt: "Define class Enemy with an int hp field, a constructor Enemy(...) that sets this.hp = 50.",
            starter: "// blueprint, the Java way\n",
            mustMatch: ["class\\s+Enemy", "int\\s+hp\\s*;", "Enemy\\s*\\([^)]*\\)\\s*\\{", "this\\.hp\\s*=\\s*50\\s*;"],
            hint: "class Enemy { private int hp; public Enemy() { this.hp = 50; } }",
          },
        },
        {
          id: "java-inherit", title: "Inheritance & Overriding", minutes: 11,
          content: [
            { t: "p", v: "extends makes one class inherit another's fields and methods. A subclass can @Override a method to specialize behaviour — a Boss is an Enemy, but hits differently." },
            { t: "code", v: 'public class Boss extends Enemy {\n    public Boss(String name) {\n        super(name);   // call the parent constructor\n    }\n\n    @Override\n    public void hit(int dmg) {\n        System.out.println("The boss shrugs off half of it");\n        super.hit(dmg / 2);\n    }\n}' },
            { t: "p", v: "This is polymorphism: code that works with Enemy automatically works with Boss, and the right hit() runs at runtime. It's the design backbone of every large Java system." },
            { t: "tip", v: "@Override isn't decoration — it makes the compiler verify you actually overrode something. Typo the method name without it and you silently created a new method instead." },
          ],
          challenge: {
            type: "pattern",
            prompt: "Declare class Boss extends Enemy, and mark an overridden method with the @Override annotation.",
            starter: "// same family, bigger health bar\n",
            mustMatch: ["class\\s+Boss\\s+extends\\s+Enemy", "@Override"],
            hint: "class Boss extends Enemy { @Override public void hit(int dmg) { ... } }",
          },
        },
        {
          id: "java-collections", title: "ArrayList & the Collections", minutes: 10,
          content: [
            { t: "p", v: "Fixed arrays are cramped; ArrayList grows on demand. The <String> in angle brackets is a generic — it tells the compiler exactly what the list holds, so nothing else can sneak in." },
            { t: "code", v: 'ArrayList<String> quests = new ArrayList<>();\nquests.add("Find the flag");\nquests.add("Defeat the boss");\n\nfor (String q : quests) {\n    System.out.println(q);\n}\nquests.size(); // 2' },
            { t: "p", v: "The enhanced for (String q : quests) visits every element — Java's for...of. Behind ArrayList sits the whole Collections framework: HashMap, HashSet, LinkedList, each with different performance trade-offs." },
            { t: "tip", v: "HashMap<String, Integer> is Java's dictionary — the next structure to learn after this." },
          ],
          challenge: {
            type: "pattern",
            prompt: "Create an ArrayList<String>, and loop over it with an enhanced for (String x : list).",
            starter: "// a list that grows\n",
            mustMatch: ["ArrayList<String>\\s+\\w+\\s*=\\s*new\\s+ArrayList<>\\s*\\(\\s*\\)", "for\\s*\\(\\s*String\\s+\\w+\\s*:\\s*\\w+\\s*\\)"],
            hint: "ArrayList<String> quests = new ArrayList<>(); … for (String q : quests) { … }",
          },
        },
        {
          id: "java-exceptions", title: "Exceptions", minutes: 10,
          content: [
            { t: "p", v: "Java splits errors into unchecked exceptions (bugs like NullPointerException) and checked exceptions (expected failures like a missing file) that the compiler FORCES you to handle — a discipline unique among mainstream languages." },
            { t: "code", v: 'try {\n    int age = Integer.parseInt(userInput);\n} catch (NumberFormatException e) {\n    System.out.println("That was not a number: " + e.getMessage());\n} finally {\n    System.out.println("Runs either way");\n}' },
            { t: "p", v: "Catch the most specific exception that applies. throw new IllegalArgumentException(\"bad hp\") raises your own — good APIs fail fast and explain why." },
            { t: "tip", v: "Never catch (Exception e) {} with an empty body. Swallowed exceptions are how systems fail mysteriously at 3am." },
          ],
          challenge: {
            type: "pattern",
            prompt: "Write a try block calling Integer.parseInt(...) with a catch for NumberFormatException that prints something.",
            starter: 'String userInput = "not a number";\n// handle it like a pro\n',
            mustMatch: ["try\\s*\\{", "Integer\\.parseInt\\s*\\(", "catch\\s*\\(\\s*NumberFormatException\\s+\\w+\\s*\\)", "System\\.out\\.println\\s*\\("],
            hint: "try { Integer.parseInt(userInput); } catch (NumberFormatException e) { System.out.println(\"nope\"); }",
          },
        },
      ],
      quiz: {
        id: "java-quiz-3",
        questions: [
          { q: "private fields exist so that…", options: ["the JVM runs faster", "outsiders use your methods, not your internals", "they use less memory", "they can't be changed"], answer: 1, why: "Encapsulation: control access through a public API." },
          { q: "class Boss extends Enemy means…", options: ["Boss copies Enemy's code once", "Boss inherits Enemy's fields & methods", "Enemy inherits from Boss", "they're unrelated"], answer: 1, why: "extends = inheritance, parent to child." },
          { q: "@Override exists to…", options: ["make methods faster", "let the compiler verify you really overrode a parent method", "hide the parent method", "document only"], answer: 1, why: "Without it, a typo silently creates a brand-new method." },
          { q: "ArrayList beats a plain array when…", options: ["you need fixed size", "the size must grow and shrink", "storing primitives only", "never"], answer: 1, why: "ArrayList resizes itself; arrays are fixed." },
          { q: "An empty catch (Exception e) {} block is…", options: ["defensive programming", "required by the compiler", "how errors vanish and haunt you later", "faster"], answer: 2, why: "Swallowing exceptions hides failures instead of handling them." },
        ],
      },
    },
  ],
};

COURSES.push(JAVA_COURSE);

PROJECTS.java = {
  id: "proj-java", xp: 150,
  brief: "Build a mini combat simulator in Java. Requirements: an Enemy class with a private int hp field, a constructor, and a takeDamage(int) method; a Boss class that extends Enemy and uses @Override on at least one method; a main method that creates both, deals some damage, and prints the battle with System.out.println. Paste the full program.",
  placeholder: "public class Enemy {\n    // fields + constructor + takeDamage\n}\n\n// class Boss extends Enemy …\n\n// main …\n",
};


/* ===================== CLOUD (AWS + AZURE PRACTITIONER) ===================== */

const CLOUD_COURSE = {
  id: "cloud",
  name: "Cloud (AWS + Azure)",
  icon: "☁️",
  tint: "#4FA8FF",
  tagline: "Practitioner-level fluency, mapped to CLF-C02 & AZ-900",
  desc: "Real cloud concepts across AWS and Azure — the vocabulary and mental models that pass certs and interviews.",
  chapters: [
    {
      id: "cloud-1", name: "Chapter 1 · Cloud Fundamentals", tier: "Beginner",
      lessons: [
        {
          id: "cl-what", title: "What Cloud Actually Is", minutes: 9,
          content: [
            { t: "p", v: "Cloud computing is renting compute, storage, and services over the internet, on demand, paying only for what you use — instead of buying and running your own servers. AWS and Azure are the two giants; the concepts below map almost 1:1 between them, only the product names differ." },
            { t: "p", v: "The killer shift is CapEx → OpEx: no huge up-front capital expenditure on hardware that sits idle, just a metered operational expense that scales with real usage. Spin up 500 servers for an hour, then delete them, and pay for exactly that hour." },
            { t: "code", v: "Six pillars of the cloud value proposition:\n  Elasticity     scale up/down automatically with demand\n  Pay-as-you-go  metered billing, no idle hardware\n  Global reach   deploy near users in minutes, worldwide\n  Agility        launch resources in seconds, not months\n  Reliability    redundancy & failover built in\n  Managed ops    the provider runs the plumbing" },
            { t: "tip", v: "Interview soundbite: 'The cloud trades capital expenditure for operational expenditure and turns capacity planning into an API call.'" },
          ],
          challenge: {
            type: "text",
            prompt: "The cloud converts large up-front hardware CapEx into ongoing, usage-based ____. (three letters)",
            answer: ["opex", "op-ex", "operational expenditure", "operating expenditure"],
            hint: "The operational-expense counterpart to CapEx.",
          },
        },
        {
          id: "cl-models", title: "IaaS, PaaS & SaaS", minutes: 10,
          content: [
            { t: "p", v: "Cloud services come in three service models, distinguished by how much the provider manages versus you. Think of it as pizza-as-a-service: make it all yourself, use a takeout kit, get it delivered, or dine out." },
            { t: "code", v: "IaaS  Infrastructure  you get raw VMs, networks, storage\n      you manage:     OS, runtime, app, data\n      examples:       AWS EC2 · Azure Virtual Machines\n\nPaaS  Platform       you get a managed runtime; deploy code\n      you manage:     just the app + data\n      examples:       AWS Elastic Beanstalk · Azure App Service\n\nSaaS  Software        finished app over the web\n      you manage:     almost nothing (settings + data)\n      examples:       Gmail · Microsoft 365 · Salesforce" },
            { t: "p", v: "As you move IaaS → PaaS → SaaS, you give up control and gain convenience. The exam LOVES asking you to classify a service into one of these three." },
            { t: "tip", v: "Serverless (AWS Lambda, Azure Functions) is a flavour of PaaS often called FaaS — Functions as a Service. You provide code; the platform provides everything else and scales to zero when idle." },
          ],
          challenge: {
            type: "text",
            prompt: "You rent a bare virtual machine and install your own OS and software on it. Which service model is this? (four letters)",
            answer: ["iaas", "i.a.a.s", "infrastructure as a service"],
            hint: "The most hands-on model — you manage the operating system yourself.",
          },
        },
        {
          id: "cl-deploy", title: "Public, Private & Hybrid", minutes: 8,
          content: [
            { t: "p", v: "Deployment models describe WHERE the cloud lives. Public cloud (AWS, Azure) is shared multi-tenant infrastructure you rent. Private cloud is dedicated to one organization — often on-premises — for control or compliance. Hybrid cloud connects the two, and multi-cloud spreads across multiple providers." },
            { t: "code", v: "Public   AWS / Azure — shared, elastic, pay-as-you-go\nPrivate  your own datacenter — full control, fixed capacity\nHybrid   both, connected — 'burst' to public at peak,\n         keep sensitive data private\nMulti    AWS + Azure + GCP — avoid lock-in, best-of-breed" },
            { t: "p", v: "Hybrid is common in the real world: a bank keeps regulated data in a private datacenter but runs its public website in the cloud, linked by a secure connection (AWS Direct Connect / Azure ExpressRoute)." },
            { t: "tip", v: "Multi-cloud is a strategy, not a product — it reduces vendor lock-in but multiplies operational complexity." },
          ],
          challenge: {
            type: "text",
            prompt: "A company keeps sensitive records in its own datacenter but runs its website on Azure, with the two connected. What deployment model is this? (one word)",
            answer: ["hybrid", "hybrid cloud"],
            hint: "A blend of private and public.",
          },
        },
        {
          id: "cl-global", title: "Regions, AZs & Edge", minutes: 10,
          content: [
            { t: "p", v: "Cloud providers run physical infrastructure worldwide, organized in a hierarchy. A Region is a geographic area (e.g. us-east-1 in Virginia, or Azure's West Europe). Within each region are multiple Availability Zones (AZs) — physically separate datacenters with independent power and networking." },
            { t: "code", v: "Region        a geographic area (us-east-1, uksouth)\n  └─ AZ       isolated datacenter(s) within the region\n  └─ AZ       (spread across these for high availability)\n  └─ AZ\n\nEdge location  CDN cache close to users (CloudFront /\n               Azure Front Door) — low-latency delivery" },
            { t: "p", v: "Deploy across multiple AZs and one datacenter catching fire doesn't take you down — that's high availability. Choose a region near your users (latency), and mind data-residency laws (some data must legally stay in-country). Edge locations cache content globally for speed." },
            { t: "tip", v: "Classic exam trap: a Region is NOT a single building. It's an area containing multiple isolated AZs. Multi-AZ = resilience; multi-Region = disaster recovery + global reach." },
          ],
          challenge: {
            type: "text",
            prompt: "Physically separate datacenters within a single region, used to build fault-tolerant apps, are called Availability ____. (one word)",
            answer: ["zones", "zone", "availability zones"],
            hint: "Region contains multiple Availability ______.",
          },
        },
      ],
      quiz: {
        id: "cloud-quiz-1",
        questions: [
          { q: "The main financial benefit of cloud is…", options: ["no internet needed", "trading CapEx for pay-as-you-go OpEx", "free compute", "unlimited storage"], answer: 1, why: "You stop buying idle hardware and pay per use." },
          { q: "AWS EC2 and Azure Virtual Machines are examples of…", options: ["SaaS", "PaaS", "IaaS", "FaaS"], answer: 2, why: "Raw VMs where you manage the OS = Infrastructure as a Service." },
          { q: "Gmail and Microsoft 365 are…", options: ["IaaS", "PaaS", "SaaS", "private cloud"], answer: 2, why: "Finished software delivered over the web = SaaS." },
          { q: "To survive a single datacenter failure you deploy across multiple…", options: ["Regions", "Availability Zones", "edge locations", "accounts"], answer: 1, why: "Multi-AZ gives high availability within a region." },
          { q: "Serverless (Lambda / Azure Functions) is often called…", options: ["IaaS", "FaaS", "SaaS", "on-prem"], answer: 1, why: "Functions as a Service — you supply only code." },
        ],
      },
    },
    {
      id: "cloud-2", name: "Chapter 2 · Core Services", tier: "Intermediate",
      lessons: [
        {
          id: "cl-compute", title: "Compute: VMs, Containers & Serverless", minutes: 11,
          content: [
            { t: "p", v: "Compute is where your code runs. There's a spectrum from most-control to least-management: virtual machines, containers, and serverless functions." },
            { t: "code", v: "VMs         full servers you rent & manage\n            AWS EC2 · Azure Virtual Machines\n\nContainers  lightweight, portable app bundles\n            AWS ECS/EKS · Azure Container Instances/AKS\n            (Kubernetes = the container orchestrator)\n\nServerless  just functions, auto-scaled, pay-per-run\n            AWS Lambda · Azure Functions\n            scales to ZERO when idle → $0 at rest" },
            { t: "p", v: "Rule of thumb: steady, long-running workloads or full OS control → VMs. Portable microservices → containers. Event-driven, spiky, or occasional tasks → serverless. Serverless is billed per invocation and per millisecond of execution, so idle time is free." },
            { t: "tip", v: "Interviewers probe trade-offs: serverless has cold-start latency and time limits; VMs cost money even when idle; containers need orchestration. Naming the trade-off beats naming the product." },
          ],
          challenge: {
            type: "text",
            prompt: "Which compute model bills per invocation, scales automatically, and costs nothing while idle? (one word)",
            answer: ["serverless", "faas", "functions", "lambda"],
            hint: "Lambda and Azure Functions are examples — it scales to zero.",
          },
        },
        {
          id: "cl-storage", title: "Storage: Object, Block & File", minutes: 11,
          content: [
            { t: "p", v: "The three storage types are a guaranteed exam topic. Object storage holds files as objects in a flat namespace with metadata — cheap, massively scalable, accessed over HTTP. Block storage is a raw virtual disk you attach to a VM. File storage is a shared network filesystem multiple machines can mount." },
            { t: "code", v: "Object  files + metadata, HTTP access, near-infinite\n        AWS S3 · Azure Blob Storage\n        use: backups, images, static sites, data lakes\n\nBlock   virtual hard disk attached to ONE VM\n        AWS EBS · Azure Managed Disks\n        use: OS disks, databases\n\nFile    shared network drive (SMB/NFS), many VMs\n        AWS EFS · Azure Files\n        use: shared app content, lift-and-shift" },
            { t: "p", v: "S3 and Blob also have storage tiers — Standard for hot data, Infrequent Access / Cool for older data, Glacier / Archive for cheap cold storage you rarely touch. Moving data to the right tier is a core cost-optimization lever." },
            { t: "tip", v: "S3 is object storage — it is NOT a filesystem you can mount like a disk. Mixing up object vs block is the classic wrong answer." },
          ],
          challenge: {
            type: "text",
            prompt: "AWS S3 and Azure Blob Storage are examples of which storage type — files with metadata over HTTP? (one word)",
            answer: ["object", "object storage"],
            hint: "Not block, not file — the flat-namespace, HTTP-accessible kind.",
          },
        },
        {
          id: "cl-network", title: "Networking: VPC, Subnets & Security Groups", minutes: 11,
          content: [
            { t: "p", v: "Your cloud resources live inside a private virtual network you define: a VPC in AWS, a VNet in Azure. You carve it into subnets — public subnets (reachable from the internet, for web servers) and private subnets (internal-only, for databases)." },
            { t: "code", v: "VPC / VNet         your isolated virtual network\n ├─ public subnet   web servers  → internet gateway\n └─ private subnet   databases    → no direct internet\n\nSecurity Group / NSG   virtual firewall on a resource\n   inbound:  allow 443 from anywhere, 22 from office IP\n   outbound: allow all\n   (deny by default — you allow what you need)" },
            { t: "p", v: "Security Groups (AWS) / Network Security Groups (Azure) are stateful virtual firewalls: they deny everything by default and you explicitly allow the ports you need. Put databases in private subnets so they're never directly exposed to the internet — a core security pattern." },
            { t: "tip", v: "Least privilege applies to networking too: open port 22 (SSH) to your office IP, never to 0.0.0.0/0 (the whole internet). That single mistake is behind countless breaches." },
          ],
          challenge: {
            type: "text",
            prompt: "The virtual firewall that controls inbound/outbound traffic to a resource, denying by default, is a Security ____ in AWS. (one word)",
            answer: ["group", "security group", "groups"],
            hint: "Azure calls it an NSG; AWS calls it a Security ______.",
          },
        },
        {
          id: "cl-db", title: "Databases: Relational vs NoSQL", minutes: 10,
          content: [
            { t: "p", v: "Managed databases let the cloud handle backups, patching, and failover so you don't run a database server yourself. They split into relational (SQL, structured, related tables — the stuff you learned in the SQL course) and NoSQL (flexible schemas, massive scale)." },
            { t: "code", v: "Relational (SQL)   structured tables, ACID, joins\n   AWS RDS (MySQL/Postgres/etc) · Azure SQL Database\n   use: orders, users, finance — anything relational\n\nNoSQL key-value    huge scale, flexible schema, fast\n   AWS DynamoDB · Azure Cosmos DB\n   use: sessions, carts, IoT, gaming leaderboards\n\nCache in-memory    microsecond reads\n   AWS ElastiCache · Azure Cache for Redis" },
            { t: "p", v: "Choose relational when data is structured and you need joins and transactions; choose NoSQL when you need enormous scale, flexible fields, and simple access patterns. Managed services trade a little control for enormous operational relief." },
            { t: "tip", v: "AWS RDS is a managed relational database service; DynamoDB is managed NoSQL. Knowing which is which is a frequent exam and interview question." },
          ],
          challenge: {
            type: "text",
            prompt: "AWS DynamoDB and Azure Cosmos DB are managed ____ databases (built for scale and flexible schemas). (one word)",
            answer: ["nosql", "no-sql", "non-relational"],
            hint: "The opposite of relational — flexible schema, massive scale.",
          },
        },
      ],
      quiz: {
        id: "cloud-quiz-2",
        questions: [
          { q: "You need to run occasional event-driven code with zero cost when idle. Best fit?", options: ["EC2 VM", "Serverless (Lambda/Functions)", "Block storage", "A private subnet"], answer: 1, why: "Serverless scales to zero and bills per run." },
          { q: "Storing millions of images accessed over HTTP calls for…", options: ["block storage (EBS)", "object storage (S3/Blob)", "file storage (EFS)", "a VPC"], answer: 1, why: "Object storage is the scalable, HTTP-accessible choice." },
          { q: "Where should a database that must never be internet-facing live?", options: ["a public subnet", "a private subnet", "an edge location", "object storage"], answer: 1, why: "Private subnets have no direct internet route." },
          { q: "A stateful virtual firewall on a resource that denies by default is a…", options: ["Region", "Security Group / NSG", "subnet", "load balancer"], answer: 1, why: "SG (AWS) / NSG (Azure) control allowed traffic." },
          { q: "AWS RDS is to relational as ____ is to NoSQL.", options: ["S3", "DynamoDB", "EC2", "VPC"], answer: 1, why: "DynamoDB is AWS's managed NoSQL database." },
        ],
      },
    },
    {
      id: "cloud-3", name: "Chapter 3 · Security, Identity & Architecture", tier: "Pro",
      lessons: [
        {
          id: "cl-shared", title: "The Shared Responsibility Model", minutes: 10,
          content: [
            { t: "p", v: "The single most important security concept on both exams. Security is SHARED: the provider secures the cloud itself; you secure what you put IN it. The exact line shifts with the service model." },
            { t: "code", v: "PROVIDER — security OF the cloud\n   physical datacenters, hardware, host OS,\n   network infrastructure, the hypervisor\n\nYOU — security IN the cloud\n   your data (always yours to protect!)\n   IAM & access config, encryption choices\n   OS patching on IaaS VMs, firewall rules,\n   application code\n\nThe more managed the service (IaaS→PaaS→SaaS),\nthe more the provider handles — but YOUR DATA and\nYOUR ACCESS CONTROL are ALWAYS your job." },
            { t: "p", v: "On an IaaS VM, patching the guest OS is YOUR responsibility. On a SaaS app, the provider patches everything — but you still control who can log in and what data goes in. Misconfigured customer-side settings (public S3 buckets!) cause the majority of cloud breaches." },
            { t: "tip", v: "Exam reflex: 'Who patches the guest OS on an EC2 instance?' → the customer. 'Who secures the physical datacenter?' → the provider. Data and access are ALWAYS the customer's." },
          ],
          challenge: {
            type: "text",
            prompt: "Under the shared responsibility model, who is responsible for patching the guest OS on an IaaS virtual machine? (one word)",
            answer: ["customer", "you", "the customer", "user", "client"],
            hint: "The provider secures the cloud; you secure what runs IN your VM.",
          },
        },
        {
          id: "cl-iam", title: "Identity & Access Management", minutes: 11,
          content: [
            { t: "p", v: "IAM controls WHO can do WHAT to WHICH resources. The building blocks: users (a person or app), groups (a bundle of users sharing permissions), roles (temporary permissions a service or user can assume), and policies (JSON documents granting or denying specific actions)." },
            { t: "code", v: "User    an identity for a person or application\nGroup   users bundled together (e.g. 'Developers')\nRole    temporary, assumable permissions — no\n        long-lived keys (a VM assumes a role to\n        read S3, safer than storing credentials)\nPolicy  the rules: allow/deny action on resource\n\nGolden rules:\n  • Least privilege — grant the minimum needed\n  • Enable MFA, especially on the root/admin account\n  • Prefer roles over long-lived access keys\n  • Never use the root account for daily work" },
            { t: "p", v: "Roles are the pro move: instead of pasting secret keys into a server (which leak), the server assumes a role and gets short-lived, auto-rotating credentials. Least privilege plus MFA plus roles is the identity security trifecta." },
            { t: "tip", v: "Interview gold: 'How would a server access S3 securely?' → Attach an IAM role, don't embed access keys. Hardcoded credentials in code is the answer they're hoping you WON'T give." },
          ],
          challenge: {
            type: "text",
            prompt: "The IAM security principle of granting only the minimum permissions required is called least ____. (one word)",
            answer: ["privilege", "least privilege"],
            hint: "You met this in the Cybersecurity course too — least ______.",
          },
        },
        {
          id: "cl-pricing", title: "Pricing & Cost Management", minutes: 10,
          content: [
            { t: "p", v: "Cloud saves money only if you manage it. Compute has several purchasing options with big price differences: on-demand (flexible, priciest), reserved / savings plans (commit 1–3 years for up to ~72% off), and spot instances (spare capacity at up to ~90% off, but can be reclaimed anytime — great for fault-tolerant batch jobs)." },
            { t: "code", v: "On-demand    pay per hour/second, no commitment  $$$$\nReserved /   commit 1–3 yrs, steady workloads      $$\nSavings Plan\nSpot         spare capacity, interruptible        $\n\nCost tools:\n  AWS Cost Explorer / Budgets · Azure Cost Management\n  AWS Free Tier / Azure free account for learning\n  Tagging resources → track spend by team/project" },
            { t: "p", v: "The mental model: predictable baseline load → reserved/savings plans; unpredictable spikes → on-demand or auto-scaling; cheap-and-interruptible batch work → spot. Set budgets and alerts so a runaway resource doesn't produce a surprise five-figure bill." },
            { t: "tip", v: "Exam favourite: spot instances are cheapest but can be terminated with little warning — only use them for workloads that tolerate interruption." },
          ],
          challenge: {
            type: "text",
            prompt: "Which purchasing option offers the deepest discount but can be reclaimed by the provider at any time? (one word)",
            answer: ["spot", "spot instances", "spot instance"],
            hint: "Spare capacity, up to ~90% off, interruptible.",
          },
        },
        {
          id: "cl-waf", title: "The Well-Architected Framework", minutes: 11,
          content: [
            { t: "p", v: "AWS's Well-Architected Framework (Azure has a near-identical CAF/WAF) defines pillars of good cloud design. Knowing them signals architectural maturity in interviews and appears on the exams." },
            { t: "code", v: "Operational Excellence  run & monitor, automate, improve\nSecurity                protect data & systems, least priv\nReliability             recover from failure, scale to demand\nPerformance Efficiency  use resources efficiently, right-size\nCost Optimization       avoid waste, pay only for value\nSustainability          minimize environmental impact\n\nHigh availability building blocks:\n  multi-AZ deployment · load balancers ·\n  auto-scaling groups · health checks · backups" },
            { t: "p", v: "To make a system reliable and available: spread it across Availability Zones, put a load balancer in front, use an auto-scaling group that adds/removes instances with demand, and health-check so unhealthy instances are replaced automatically. That pattern — LB + auto-scaling + multi-AZ — answers most 'design a resilient web app' questions." },
            { t: "tip", v: "If an interviewer asks 'how do you make this highly available?', the reliable answer is: multiple Availability Zones, a load balancer, and auto-scaling with health checks." },
          ],
          challenge: {
            type: "text",
            prompt: "Which Well-Architected pillar covers recovering from failure and scaling to meet demand? (one word)",
            answer: ["reliability", "reliable"],
            hint: "The pillar about resilience, failover, and handling load.",
          },
        },
      ],
      quiz: {
        id: "cloud-quiz-3",
        questions: [
          { q: "Under shared responsibility, protecting the physical datacenter is…", options: ["the customer's job", "the provider's job", "shared equally", "nobody's job"], answer: 1, why: "Security OF the cloud (hardware, facilities) is the provider's." },
          { q: "Who is ALWAYS responsible for the data you store, in every service model?", options: ["the provider", "the customer", "it depends on the region", "the ISP"], answer: 1, why: "Your data and access control are always yours." },
          { q: "The safest way for a VM to access S3 is to…", options: ["hardcode access keys in the app", "assume an IAM role", "make the bucket public", "email the credentials"], answer: 1, why: "Roles give short-lived, auto-rotating credentials — no embedded secrets." },
          { q: "Cheapest compute for an interruptible batch job is…", options: ["on-demand", "reserved", "spot", "dedicated host"], answer: 2, why: "Spot uses spare capacity at deep discounts but can be reclaimed." },
          { q: "A highly available web app typically uses a load balancer, auto-scaling, and…", options: ["a single big server", "multiple Availability Zones", "one edge location", "spot only"], answer: 1, why: "Multi-AZ removes the single-datacenter failure point." },
          { q: "Which is NOT a Well-Architected pillar?", options: ["Security", "Reliability", "Cost Optimization", "Maximum Vendor Lock-in"], answer: 3, why: "The pillars promote good design; lock-in is the opposite of that." },
        ],
      },
    },
  ],
};

COURSES.push(CLOUD_COURSE);

PROJECTS.cloud = {
  id: "proj-cloud", xp: 150,
  brief: "Cloud architecture design (written). Scenario: design a highly available, secure, cost-aware architecture for a photo-sharing web app expecting spiky traffic, on AWS OR Azure. Your answer must address: (1) COMPUTE choice and why; (2) STORAGE for the photos and why that type; (3) DATABASE choice; (4) how you achieve HIGH AVAILABILITY (name specific mechanisms); (5) at least two SECURITY measures using the shared responsibility model and IAM; (6) one COST optimization. Use real service names (EC2/S3/RDS/etc or Azure equivalents). Write it as a short design doc.",
  placeholder: "COMPUTE: ...\nSTORAGE (photos): ...\nDATABASE: ...\nHIGH AVAILABILITY: ...\nSECURITY (x2): ...\nCOST OPTIMIZATION: ...\n",
};


/* ===================== INTERVIEW GYM DATA ===================== */

/* AI mock-interview tracks — Claude plays the interviewer */
const INTERVIEW_TRACKS = [
  { id: "behavioral", icon: "🗣️", name: "Behavioral", tint: T.violet, blurb: "STAR-method stories, teamwork, conflict, failure. The round most people underprepare.",
    sys: "behavioral interview questions (teamwork, conflict, failure, leadership, ownership). Push for STAR structure (Situation, Task, Action, Result) and concrete specifics." },
  { id: "coding", icon: "⌨️", name: "Coding / DSA", tint: T.cyan, blurb: "Algorithm problems: arrays, strings, hash maps, two pointers. Explain your approach and complexity.",
    sys: "coding/data-structures-and-algorithms interview questions at easy-to-medium difficulty (arrays, strings, hashmaps, two-pointers, recursion). Ask them to describe an approach and its time/space complexity in plain language or pseudocode; you are NOT running code." },
  { id: "systemdesign", icon: "🏗️", name: "System Design", tint: T.amber, blurb: "Design a URL shortener, a chat app, a feed. Junior-friendly scope, real trade-offs.",
    sys: "entry/junior-level system design questions (design a URL shortener, a rate limiter, a news feed, a photo service). Guide them to cover requirements, core components, data storage, and one scaling or trade-off consideration." },
  { id: "python", icon: "🐍", name: "Python Technical", tint: T.amber, blurb: "Language internals: mutability, comprehensions, generators, the GIL, decorators.",
    sys: "Python technical interview questions (data structures, mutability, comprehensions, generators, decorators, the GIL, common pitfalls)." },
  { id: "javascript", icon: "⚡", name: "JavaScript Technical", tint: T.violet, blurb: "Closures, the event loop, promises/async, hoisting, this. The famous tricky bits.",
    sys: "JavaScript technical interview questions (closures, the event loop, promises/async-await, hoisting, this binding, == vs ===, prototypes)." },
  { id: "java", icon: "☕", name: "Java Technical", tint: "#FF8A3D", blurb: "OOP pillars, equals/hashCode, collections, checked vs unchecked exceptions, the JVM.",
    sys: "Java technical interview questions (OOP principles, equals vs ==, hashCode contract, collections framework, exceptions, interfaces vs abstract classes, the JVM)." },
  { id: "sql", icon: "🗄️", name: "SQL Technical", tint: T.cyan, blurb: "Joins, indexes, normalization, N+1, query optimization, transactions.",
    sys: "SQL technical interview questions (joins, indexes, normalization, GROUP BY vs WHERE vs HAVING, query optimization, transactions/ACID, SQL injection defense)." },
  { id: "cloud", icon: "☁️", name: "Cloud Technical", tint: "#4FA8FF", blurb: "Shared responsibility, HA design, IAM, storage types, cost trade-offs.",
    sys: "cloud (AWS/Azure) technical interview questions (shared responsibility model, high availability design, IAM & least privilege, storage/compute trade-offs, cost optimization)." },
  { id: "cyber", icon: "🛡️", name: "Security Technical", tint: T.green, blurb: "OWASP, hashing vs encryption, XSS/SQLi defense, incident response, auth.",
    sys: "cybersecurity technical interview questions (OWASP top 10, hashing vs encryption, XSS and SQL injection defenses, authentication/MFA, incident response, defense in depth). Keep everything defensive." },
];

/* Curated deep flashcard bank — real interview Q&A, no AI needed */
const FLASHCARDS = {
  "Big-O & Complexity": [
    { q: "What does Big-O notation describe?", a: "How an algorithm's time or space grows as the input size n grows — the worst-case upper bound. It ignores constants and lower-order terms, focusing on scalability, not raw speed." },
    { q: "Order these from fastest to slowest growth: O(n²), O(1), O(log n), O(n), O(n log n).", a: "O(1) < O(log n) < O(n) < O(n log n) < O(n²). Constant beats logarithmic beats linear beats linearithmic beats quadratic." },
    { q: "What's the time complexity of a hash map lookup, and the catch?", a: "O(1) average case. The catch: worst case is O(n) if many keys collide into the same bucket. Interviewers love when you mention the worst case." },
    { q: "Binary search complexity and its one requirement?", a: "O(log n) — it halves the search space each step. Requirement: the array must be SORTED first (sorting itself is O(n log n))." },
    { q: "Why is a nested loop over the same array usually O(n²)?", a: "The inner loop runs n times for each of the n outer iterations → n × n = n² total operations. A red flag to look for a hash-map optimization." },
  ],
  "Data Structures": [
    { q: "Array vs Linked List — key trade-off?", a: "Arrays: O(1) index access, but O(n) insert/delete in the middle (shifting). Linked lists: O(1) insert/delete given the node, but O(n) to find an element (no indexing). Arrays win on cache locality." },
    { q: "When would you reach for a hash map / dictionary?", a: "When you need fast O(1) average lookups, insertions, and deletions by key — deduplication, counting frequencies, caching, or turning an O(n²) nested-loop scan into O(n)." },
    { q: "Stack vs Queue?", a: "Stack = LIFO (last in, first out) — undo, call stack, DFS, matching brackets. Queue = FIFO (first in, first out) — task scheduling, BFS, buffering." },
    { q: "What problem does a hash set solve that a list doesn't?", a: "O(1) membership tests ('have I seen this before?') and automatic deduplication. Checking 'in' on a list is O(n); on a set it's O(1) average." },
    { q: "When is a tree (like a binary search tree) the right structure?", a: "When you need sorted data with O(log n) search, insert, and delete (balanced), or hierarchical relationships. Also underpins databases (B-trees) and priority queues (heaps)." },
  ],
  "Coding Patterns": [
    { q: "Explain the two-pointer technique.", a: "Use two indices moving through a structure — often from both ends inward, or one fast + one slow — to solve problems in O(n) without extra space. Great for sorted-array pair sums, palindromes, and reversing in place." },
    { q: "How do you find a duplicate in an array efficiently?", a: "Iterate once, storing seen values in a hash set; if a value is already in the set, it's the duplicate. O(n) time, O(n) space — versus the naive O(n²) nested loop." },
    { q: "What is the sliding-window pattern good for?", a: "Problems over contiguous subarrays/substrings — longest substring without repeats, max sum of k consecutive elements. Grow/shrink a window instead of recomputing, turning O(n²) into O(n)." },
    { q: "How do you reverse a string in place, and what's the complexity?", a: "Two pointers at the ends, swap, move inward until they meet. O(n) time, O(1) extra space. (In Python, strings are immutable — you'd reverse a list of chars.)" },
    { q: "Recursion vs iteration — when to pick recursion?", a: "Recursion shines on self-similar/nested structures (trees, graphs, divide-and-conquer). Downsides: call-stack memory and overhead. Always define the base case first, then the shrinking step." },
  ],
  "Python": [
    { q: "Mutable vs immutable — name examples and why it matters.", a: "Immutable: int, float, str, tuple. Mutable: list, dict, set. It matters for function arguments (a mutated list persists outside the function) and for dict keys (only immutable/hashable types allowed)." },
    { q: "What is a list comprehension and why prefer it?", a: "A concise one-line way to build a list: [x*2 for x in nums if x > 0]. It's more readable and often faster than an append loop for simple transformations." },
    { q: "Generator vs list — what's the advantage?", a: "A generator (yield, or (x for x in ...)) produces items lazily one at a time instead of building the whole list in memory — huge memory savings for large or infinite sequences." },
    { q: "What is the GIL?", a: "The Global Interpreter Lock lets only one thread execute Python bytecode at a time, so threads don't give true CPU parallelism. Use multiprocessing for CPU-bound work; threads/async are fine for I/O-bound work." },
    { q: "What does a decorator do?", a: "It's a function that wraps another function to add behaviour (logging, timing, auth) without modifying it — the @decorator syntax. It leans on closures and functions being first-class objects." },
  ],
  "JavaScript": [
    { q: "Explain the event loop.", a: "JS is single-threaded. Synchronous code runs on the call stack; async callbacks wait in queues (microtasks like promises first, then macrotasks like setTimeout) and run only when the stack is empty. That's how non-blocking I/O works." },
    { q: "What is a closure and a real use?", a: "A function that remembers variables from its defining scope even after that scope returns. Uses: private state (module pattern, counters), and callbacks that capture context." },
    { q: "== vs === ?", a: "== does type coercion before comparing (0 == '' is true, surprisingly); === compares value AND type with no coercion. Always use === unless you have a specific reason not to." },
    { q: "var vs let vs const, including hoisting.", a: "var is function-scoped and hoisted (initialized as undefined); let/const are block-scoped and hoisted but sit in a 'temporal dead zone' until declared. Default to const, use let when reassigning, avoid var." },
    { q: "Promise vs async/await?", a: "Both handle async work. async/await is syntactic sugar over promises — it reads like synchronous code, uses try/catch for errors, and avoids '.then' chaining. Under the hood it's still promises." },
  ],
  "Java": [
    { q: "The four pillars of OOP?", a: "Encapsulation (hide internals behind methods), Inheritance (extend a class), Polymorphism (one interface, many implementations — overriding), and Abstraction (expose essentials, hide complexity)." },
    { q: "Why must equals() and hashCode() be overridden together?", a: "The contract: equal objects MUST return equal hash codes. Break it and objects vanish or duplicate in HashMaps/HashSets. Override both or neither." },
    { q: "Interface vs abstract class?", a: "An interface is a pure contract (a class can implement many); an abstract class can hold shared state and partial implementation but a class extends only one. 'Can-do' capability → interface; 'is-a' with shared code → abstract class." },
    { q: "Checked vs unchecked exceptions?", a: "Checked (e.g. IOException) must be declared or caught — the compiler enforces it, for recoverable/expected failures. Unchecked (RuntimeException, NullPointerException) signal bugs and aren't forced." },
    { q: "== vs .equals() for objects in Java?", a: "== compares references (are these the SAME object in memory?); .equals() compares logical value/content. For Strings and objects, use .equals() — == 'working' is often a pooling coincidence." },
  ],
  "SQL & Databases": [
    { q: "INNER JOIN vs LEFT JOIN?", a: "INNER JOIN returns only rows with a match in both tables. LEFT JOIN returns all left-table rows, filling NULLs where the right table has no match. Great for 'find records WITHOUT a related row'." },
    { q: "What does an index do, and the trade-off?", a: "An index (usually a B-tree) makes lookups/filters/sorts on a column fast — O(log n) instead of a full O(n) scan. Trade-off: it uses storage and slows down writes (INSERT/UPDATE must maintain it)." },
    { q: "WHERE vs HAVING?", a: "WHERE filters rows BEFORE grouping; HAVING filters groups AFTER aggregation. You can't use an aggregate like COUNT(*) in WHERE — that's HAVING's job." },
    { q: "What is the N+1 query problem?", a: "Running 1 query to fetch a list, then 1 more query per row (N of them) to fetch related data — N+1 round trips. Fix with a JOIN or a single batched IN query." },
    { q: "What does ACID stand for?", a: "Atomicity (all-or-nothing), Consistency (valid state to valid state), Isolation (concurrent transactions don't interfere), Durability (committed data survives crashes). The guarantees relational transactions provide." },
  ],
};


// Derived once here (was recomputed in "ENGINE HELPERS" in the original) --
// flat list of every lesson across every course/chapter, tagged with its
// parent courseId, used for "next lesson" lookups and badge counting.
export const allLessons = COURSES.flatMap((c) =>
  c.chapters.flatMap((ch) => ch.lessons.map((l) => ({ ...l, courseId: c.id })))
);

export {
  TITLES,
  levelFromXp,
  xpForLevel,
  titleForLevel,
  BADGES,
  COURSES,
  TERMINAL_FS,
  LABS,
  PROJECTS,
  INTERVIEW_TRACKS,
  FLASHCARDS,
};
