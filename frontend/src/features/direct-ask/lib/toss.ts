"use client";

import { useEffect, useState } from "react";

/**
 * Toss Payments' hosted checkout ("결제창") API (Phase 25, ADR-0037) — requestPayment() redirects
 * the browser to Toss's own page for card entry, then back to successUrl/failUrl with
 * paymentKey/orderId/amount as query params. No card data ever touches this app.
 */
interface TossPaymentsInstance {
  requestPayment(method: "카드", params: {
    amount: number;
    orderId: string;
    orderName: string;
    successUrl: string;
    failUrl: string;
  }): Promise<void>;
}

declare global {
  interface Window {
    TossPayments?: (clientKey: string) => TossPaymentsInstance;
  }
}

const SCRIPT_SRC = "https://js.tosspayments.com/v1/payment";
let loadPromise: Promise<void> | null = null;

function loadTossScript(): Promise<void> {
  if (typeof window !== "undefined" && window.TossPayments) {
    return Promise.resolve();
  }
  if (!loadPromise) {
    loadPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = SCRIPT_SRC;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error("Failed to load Toss Payments SDK"));
      document.head.appendChild(script);
    });
  }
  return loadPromise;
}

/** Returns null until the SDK script has loaded — callers should disable the pay button until then. */
export function useTossPayments(clientKey: string | undefined): TossPaymentsInstance | null {
  const [instance, setInstance] = useState<TossPaymentsInstance | null>(null);

  useEffect(() => {
    if (!clientKey) return;
    let cancelled = false;
    loadTossScript()
      .then(() => {
        if (!cancelled && window.TossPayments) {
          setInstance(window.TossPayments(clientKey));
        }
      })
      .catch(() => {
        // instance stays null — caller's "결제창을 여는 중" state simply never resolves
      });
    return () => {
      cancelled = true;
    };
  }, [clientKey]);

  return instance;
}
