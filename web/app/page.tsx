import { Suspense } from "react";
import { getDataset } from "@/lib/api";

async function DataPreview() {
  const { data, source } = await getDataset("sample");

  return (
    <>
      <p className="mt-6 text-sm">
        Источник данных: <strong>{source === "api" ? "API" : "мок (web/mocks)"}</strong>
      </p>
      <pre className="mt-2 overflow-x-auto rounded bg-gray-100 p-4 text-xs text-gray-800">
        {JSON.stringify(data, null, 2)}
      </pre>
    </>
  );
}

export default function Home() {
  return (
    <main className="mx-auto max-w-3xl p-8">
      <h1 className="text-3xl font-bold">NASA Space Apps Challenge</h1>
      <p className="mt-2 text-gray-600">
        Каркас проекта. Заменить на интерфейс вашего решения.
      </p>
      <Suspense fallback={<p className="mt-6 text-sm">Загрузка данных…</p>}>
        <DataPreview />
      </Suspense>
    </main>
  );
}
