
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiRequest } from "../services/api";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const data = await apiRequest("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });

      const token = data.token ?? data.accessToken;

      if (!token) {
        throw new Error(
          "Login succeeded, but no token was found in the response."
        );
      }

      localStorage.setItem("token", data.accessToken);
      localStorage.setItem(
        "user",
        JSON.stringify({
          userId: data.userId,
          shopId: data.shopId,
          name: data.name,
          email: data.email,
          role: data.role,
        })
      );
  window.dispatchEvent(new Event("auth-change"));
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err.message || "Unable to log in. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <form className="content-card login-card" onSubmit={handleSubmit}>
        <p className="eyebrow">BILLING MANAGEMENT</p>
        <h1>Welcome back</h1>
        <p className="muted">
          Sign in to manage your shop's billing.
        </p>

        <div className="form-group">
          <label htmlFor="loginEmail">Email Address</label>
          <input
            id="loginEmail"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="Enter your email"
            autoComplete="username"
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="loginPassword">Password</label>
          <input
            id="loginPassword"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Enter your password"
            autoComplete="current-password"
            required
          />
        </div>

        {error && (
          <p role="alert" className="form-error">
            {error}
          </p>
        )}

        <button
          type="submit"
          className="primary-button"
          disabled={loading}
        >
          {loading ? "Signing in..." : "Sign In"}
        </button>
      </form>
    </div>
  );
}