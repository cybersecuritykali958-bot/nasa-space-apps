import sample from "@/mocks/sample.json";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8000";

/** Загружает набор данных из API, при ошибке возвращает мок (для разработки и демо без сети). */
export async function getDataset(name: string) {
  try {
    const res = await fetch(`${API_URL}/api/datasets/${name}`, { cache: "no-store" });
    if (!res.ok) throw new Error(`API ${res.status}`);
    return { data: await res.json(), source: "api" as const };
  } catch {
    return { data: sample, source: "mock" as const };
  }
}
