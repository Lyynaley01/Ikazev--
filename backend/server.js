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
app.use("/answer", rateLimit({ windowMs: 15 * 60 * 1000, limit: 10, standardHeaders: "draft-8", legacyHeaders: false, message: { error: "Terlalu banyak request. Coba lagi nanti." } }));
app.get("/", (_req, res) => res.json({ service: "Ikazev Gemini API", status: "online" }));

app.post("/answer", async (req, res) => {
  if (!API_KEY) return res.status(503).json({ error: "Backend belum dikonfigurasi." });
  const question = req.body?.question;
  const targetScore = Number(req.body?.targetScore ?? 80);
  if (typeof question !== "string" || !question.trim() || question.length > 12000)
    return res.status(400).json({ error: "Pertanyaan kosong atau terlalu panjang." });
  if (!Number.isFinite(targetScore) || targetScore < 1 || targetScore > 100)
    return res.status(400).json({ error: "Target nilai harus 1–100." });

  try {
    const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-goog-api-key": API_KEY },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text:
          "Bantu pengguna berlatih pada aplikasi kuis miliknya sendiri. Jawab berdasarkan teks soal dan jangan mengarang bila OCR tidak lengkap. Target nilai pengguna: " + targetScore + "/100. " +
          "Klasifikasikan soal sebagai choice untuk pilihan ganda, text untuk isian/esai. Kembalikan JSON valid saja tanpa markdown dengan schema: {\"type\":\"choice\"|\"text\",\"answer\":\"jawaban yang tepat\",\"explanation\":\"alasan singkat\",\"confidence\":0.0}. Untuk pilihan ganda, answer harus berupa teks opsi yang tampak di OCR, bukan hanya huruf jika teks opsi tersedia. Untuk isian/esai, answer berisi jawaban yang bisa dimasukkan ke field. Jika soal tidak lengkap atau pilihan tidak terbaca, answer harus kosong dan jelaskan kendalanya. Target nilai bukan alasan untuk sengaja menjawab salah.\n\nTeks layar/OCR:\n" + question.trim()
        }] }],
        generationConfig: { temperature: 0.1, maxOutputTokens: 1200, responseMimeType: "application/json" }
      }),
      signal: AbortSignal.timeout(35000)
    });
    const data = await response.json();
    if (!response.ok) {
      console.error("Gemini returned HTTP", response.status);
      return res.status(502).json({ error: "Gemini gagal memproses permintaan. Coba lagi nanti." });
    }
    const answer = data?.candidates?.[0]?.content?.parts?.map(part => part.text || "").join("").trim();
    if (!answer) return res.status(502).json({ error: "Gemini tidak mengembalikan jawaban teks." });
    try {
      const parsed = JSON.parse(answer);
      if (!['choice', 'text'].includes(parsed.type) || typeof parsed.answer !== 'string') throw new Error('schema');
      return res.json({ answer: JSON.stringify({ type: parsed.type, answer: parsed.answer, explanation: String(parsed.explanation || ''), confidence: Number(parsed.confidence || 0) }) });
    } catch {
      return res.status(502).json({ error: "Format jawaban AI tidak valid. Coba ulangi capture." });
    }
  } catch (error) {
    console.error("Gemini request error:", error.name || "UnknownError");
    return res.status(502).json({ error: "Layanan AI sedang tidak tersedia. Coba lagi nanti." });
  }
});
app.listen(PORT, "0.0.0.0", () => console.log(`Ikazev backend listening on port ${PORT}`));
