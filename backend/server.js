import express from "express";
import helmet from "helmet";
import { rateLimit } from "express-rate-limit";

const app = express();
const PORT = process.env.PORT || 3000;
const API_KEY = process.env.GEMINI_API_KEY;
const MODEL = process.env.GEMINI_MODEL || "gemini-3.8-flash";

app.disable("x-powered-by");
app.use(helmet());
app.use(express.json({ limit: "20kb" }));

app.use("/answer", rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 10,
  standardHeaders: "draft-8",
  legacyHeaders: false,
  message: { error: "Terlalu banyak request. Coba lagi nanti." }
}));

app.get("/", (_req, res) => {
  res.json({ service: "Ikazev Gemini API", status: "online" });
});

app.post("/answer", async (req, res) => {
  if (!API_KEY) {
    return res.status(503).json({
      error: "Backend belum dikonfigurasi."
    });
  }

  const question = req.body?.question;

  if (typeof question !== "string" ||
      question.trim().length === 0 ||
      question.length > 12000) {
    return res.status(400).json({
      error: "Pertanyaan kosong atau terlalu panjang."
    });
  }

  try {
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": API_KEY
        },
        body: JSON.stringify({
          contents: [{
            role: "user",
            parts: [{
              text:
                "Kamu adalah asisten yang membantu menjawab pertanyaan. " +
                "Jawab dalam bahasa Indonesia kecuali diperlukan bahasa lain. " +
                "Jelaskan secara ringkas, akurat, dan nyatakan jika tidak yakin.\n\n" +
                "Pertanyaan:\n" + question.trim()
            }]
          }],
          generationConfig: {
            temperature: 0.2,
            maxOutputTokens: 1200
          }
        }),
        signal: AbortSignal.timeout(35000)
      }
    );

    const data = await response.json();

    if (!response.ok) {
      console.error("Gemini returned HTTP", response.status);
      return res.status(502).json({
        error: "Gemini gagal memproses permintaan. Coba lagi nanti."
      });
    }

    const answer = data?.candidates?.[0]?.content?.parts
      ?.map(part => part.text || "")
      .join("")
      .trim();

    if (!answer) {
      return res.status(502).json({
        error: "Gemini tidak mengembalikan jawaban teks."
      });
    }

    return res.json({ answer });
  } catch (error) {
    console.error("Gemini request error:", error.name || "UnknownError");
    return res.status(502).json({
      error: "Layanan AI sedang tidak tersedia. Coba lagi nanti."
    });
  }
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Ikazev backend listening on port ${PORT}`);
});
