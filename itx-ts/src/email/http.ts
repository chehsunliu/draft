import { EmailClient } from "../types.js";
import { env } from "../util.js";

export class HttpEmailClient implements EmailClient {
  private readonly url = env("ITX_EMAIL_URL");
  private readonly apiKey = env("ITX_EMAIL_API_KEY");

  async send(message: {
    to: string;
    subject: string;
    body: string;
  }): Promise<void> {
    const response = await fetch(this.url, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${this.apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(message),
    });
    if (!response.ok) {
      throw new Error(
        `email API returned ${response.status}: ${await response.text()}`,
      );
    }
  }
}
