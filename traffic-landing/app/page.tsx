import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ArrowRight, Server, Shield, Gauge } from "lucide-react";

export default function RateLimiterLanding() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Hero */}
      <section className="max-w-6xl mx-auto px-6 py-24 grid gap-10">
        <div className="space-y-4">
          <h1 className="text-6xl font-bold tracking-tight leading-none">
            Traffic <span aria-hidden>🚦</span>
          </h1>
          <p className="text-2xl font-semibold text-slate-200">
            Distributed Rate Limiting for Shared APIs
          </p>
        </div>

        <p className="text-xl text-slate-300 max-w-3xl">
          A centralized, fault-tolerant rate limiting service that protects shared
          APIs from abuse, traffic spikes, and accidental overload without embedding
          rate-limiting logic into every application.
        </p>

        <div className="flex flex-wrap gap-4">
          {/* Get Started -> scroll to Installation */}
          <Button asChild className="text-lg px-6 py-5">
            <a href="#installation">Get Started</a>
          </Button>

          {/* View on GitHub -> open repo */}
          <Button
            asChild
            className="text-lg px-6 py-5 bg-transparent text-slate-100 border border-slate-600 hover:bg-slate-900 hover:border-slate-500"
          >
            <a
              href="https://github.com/shlokmestry/traffic"
              target="_blank"
              rel="noreferrer"
            >
              View on GitHub <ArrowRight className="ml-2 h-4" />
            </a>
          </Button>
        </div>
      </section>

      {/* Value Props */}
      <section className="max-w-6xl mx-auto px-6 py-20 grid md:grid-cols-3 gap-6">
        <Card className="bg-white text-slate-900">
          <CardContent className="p-6 space-y-2">
            <Shield className="text-slate-900" />
            <h3 className="text-xl font-semibold">Fail-Closed Safety</h3>
            <p className="text-slate-600">
              Protect downstream systems even when Redis or dependencies fail.
            </p>
          </CardContent>
        </Card>

        <Card className="bg-white text-slate-900">
          <CardContent className="p-6 space-y-2">
            <Gauge className="text-slate-900" />
            <h3 className="text-xl font-semibold">Low-Latency Decisions</h3>
            <p className="text-slate-600">
              Token Bucket enforcement using atomic Redis Lua scripts.
            </p>
          </CardContent>
        </Card>

        <Card className="bg-white text-slate-900">
          <CardContent className="p-6 space-y-2">
            <Server className="text-slate-900" />
            <h3 className="text-xl font-semibold">Centralized Control</h3>
            <p className="text-slate-600">
              Manage traffic rules once, apply them everywhere.
            </p>
          </CardContent>
        </Card>
      </section>

      {/* How it Works */}
      <section className="max-w-6xl mx-auto px-6 py-20">
        <h2 className="text-3xl font-bold mb-8">How It Works</h2>

        <div className="grid md:grid-cols-3 gap-6">
          <Card className="bg-white text-slate-900">
            <CardContent className="p-6">
              <h4 className="font-semibold mb-2">1. Define Rules</h4>
              <p className="text-slate-600">
                Create rate limit rules per user, IP, API key, endpoint, or plan.
              </p>
            </CardContent>
          </Card>

          <Card className="bg-white text-slate-900">
            <CardContent className="p-6">
              <h4 className="font-semibold mb-2">2. Enforce at Gateway</h4>
              <p className="text-slate-600">
                API Gateway calls the rate limiter before forwarding traffic.
              </p>
            </CardContent>
          </Card>

          <Card className="bg-white text-slate-900">
            <CardContent className="p-6">
              <h4 className="font-semibold mb-2">3. Atomic Decisions</h4>
              <p className="text-slate-600">
                Redis-backed Token Bucket decides allow / deny in one operation.
              </p>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Installation */}
      <section
        id="installation"
        className="max-w-6xl mx-auto px-6 py-20 scroll-mt-24"
      >
        <h2 className="text-3xl font-bold mb-6">Installation</h2>

        <p className="text-slate-300 mb-6">
          You can run this service using Docker (recommended), or locally via Maven.
        </p>

        <div className="grid gap-10">
          <div className="space-y-4">
            <h3 className="text-xl font-semibold text-slate-100">
              Option A: Run with Docker Compose (recommended)
            </h3>

            <p className="text-slate-300">
              This repo includes a docker-compose.yml that runs:
            </p>

            <ul className="space-y-2 text-slate-300">
              <li>• Redis</li>
              <li>• traffic-service (built from this repo)</li>
            </ul>

            <Card className="overflow-hidden bg-slate-900 border border-slate-800 shadow-[0_0_0_1px_rgba(255,255,255,0.04)_inset]">
              <CardContent className="p-0">
                <pre className="p-6 text-slate-50 text-sm font-mono whitespace-pre-wrap break-words">
                  {`docker compose up --build`}
                </pre>
              </CardContent>
            </Card>
          </div>

          <div className="space-y-4">
            <h3 className="text-xl font-semibold text-slate-100">
              Option B: Build and run a Docker image
            </h3>

            <p className="text-slate-300">
              If you have a Dockerfile in the repo root:
            </p>

            <Card className="overflow-hidden bg-slate-900 border border-slate-800 shadow-[0_0_0_1px_rgba(255,255,255,0.04)_inset]">
              <CardContent className="p-0">
                <pre className="p-6 text-slate-50 text-sm font-mono whitespace-pre-wrap break-words">
                  {`docker build -t traffic-service:local .

docker run --rm -p 8081:8081 \\
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \\
  -e SPRING_DATA_REDIS_PORT=6379 \\
  traffic-service:local`}
                </pre>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

     

      {/* Footer */}
      <footer className="border-t border-slate-800 py-10 px-6 text-center text-slate-400">
        <p className="text-slate-300">
          Built to keep your APIs calm under pressure, one token at a time.
        </p>
        <p className="mt-2 text-sm text-slate-500">© 2026 Traffic</p>
      </footer>
    </div>
  );
}
