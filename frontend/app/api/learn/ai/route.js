/**
 * Server-side proxy for TechAway's AI features (tutor chat, code review,
 * mock-interview questions/grading, final-project grading).
 *
 * The original TechAway prototype called api.anthropic.com directly from
 * the browser with no API key attached — that only works inside the
 * Anthropic Artifacts sandbox, which injects credentials at the fetch
 * layer. There is no equivalent in a real deployment, and shipping a
 * client-side call to a paid third-party API with no key would either be
 * dead code or (worse) tempt someone into hardcoding a secret into the
 * bundle. This route holds the key server-side instead.
 *
 * ANTHROPIC_API_KEY is not currently set anywhere in this project (it's a
 * new dependency this feature introduces), so until someone adds it to the
 * environment, every AI-powered surface in /learn degrades to a clear
 * "AI features aren't configured yet" state instead of a silent failure or
 * a fake response.
 */

const MODEL = "claude-sonnet-4-5";

export async function GET() {
  return Response.json({ available: !!process.env.ANTHROPIC_API_KEY });
}

export async function POST(request) {
  const apiKey = process.env.ANTHROPIC_API_KEY;
  if (!apiKey) {
    return Response.json(
      { error: "AI features aren't configured on this deployment yet (no ANTHROPIC_API_KEY set)." },
      { status: 503 }
    );
  }

  let body;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "Invalid request body." }, { status: 400 });
  }

  const prompt = typeof body?.prompt === "string" ? body.prompt.slice(0, 8000) : "";
  if (!prompt.trim()) {
    return Response.json({ error: "Missing prompt." }, { status: 400 });
  }

  try {
    const resp = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": apiKey,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify({
        model: MODEL,
        max_tokens: 1000,
        messages: [{ role: "user", content: prompt }],
      }),
    });

    const data = await resp.json();
    if (!resp.ok) {
      return Response.json(
        { error: data?.error?.message || "The AI service returned an error." },
        { status: resp.status }
      );
    }

    const text = (data.content || [])
      .filter((b) => b.type === "text")
      .map((b) => b.text)
      .join("\n");

    if (!text) {
      return Response.json({ error: "The AI service returned an empty response." }, { status: 502 });
    }

    return Response.json({ text });
  } catch {
    return Response.json({ error: "Couldn't reach the AI service." }, { status: 502 });
  }
}
