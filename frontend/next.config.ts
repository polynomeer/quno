import type { NextConfig } from "next";
import createBundleAnalyzer from "@next/bundle-analyzer";

const nextConfig: NextConfig = {
  // Docker 이미지에서 node_modules 전체 없이 최소 산출물만으로 서버를 띄우기 위함
  // (backend/Dockerfile과 대칭되는 frontend/Dockerfile이 .next/standalone을 그대로 복사한다).
  output: "standalone",
};

// ANALYZE=true npm run build로 번들 구성을 시각화한다(quality-improvement-plan.md Q-2).
const withBundleAnalyzer = createBundleAnalyzer({
  enabled: process.env.ANALYZE === "true",
});

export default withBundleAnalyzer(nextConfig);
