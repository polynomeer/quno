import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Docker 이미지에서 node_modules 전체 없이 최소 산출물만으로 서버를 띄우기 위함
  // (backend/Dockerfile과 대칭되는 frontend/Dockerfile이 .next/standalone을 그대로 복사한다).
  output: "standalone",
};

export default nextConfig;
