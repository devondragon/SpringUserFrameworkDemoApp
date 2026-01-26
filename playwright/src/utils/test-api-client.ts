import { APIRequestContext, request } from '@playwright/test';

/**
 * Response types for Test API endpoints.
 */
export interface UserExistsResponse {
  exists: boolean;
  email: string;
}

export interface UserEnabledResponse {
  exists: boolean;
  enabled: boolean;
  email: string;
}

export interface UserDetailsResponse {
  exists: boolean;
  email: string;
  firstName?: string;
  lastName?: string;
  enabled?: boolean;
  locked?: boolean;
  failedLoginAttempts?: number;
}

export interface VerificationTokenResponse {
  exists: boolean;
  email: string;
  token: string | null;
  expiryDate?: string;
}

export interface PasswordResetTokenResponse {
  exists: boolean;
  email: string;
  token: string | null;
  expiryDate?: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  enabled?: boolean;
}

export interface CreateUserResponse {
  success: boolean;
  id?: number;
  email: string;
  enabled?: boolean;
  error?: string;
}

export interface DeleteUserResponse {
  success: boolean;
  email: string;
  error?: string;
}

export interface EnableUserResponse {
  success: boolean;
  email: string;
  enabled?: boolean;
  error?: string;
}

export interface UnlockUserResponse {
  success: boolean;
  email: string;
  locked?: boolean;
  error?: string;
}

export interface CreateVerificationTokenResponse {
  success: boolean;
  email: string;
  token?: string;
  expiryDate?: string;
  error?: string;
}

export interface HealthResponse {
  status: string;
  profile: string;
  timestamp: string;
}

/**
 * Client for interacting with the Test API endpoints.
 * Used to manage test data and validate database state during E2E tests.
 */
export class TestApiClient {
  private baseUrl: string;
  private context: APIRequestContext | null = null;

  constructor(baseUrl: string = process.env.BASE_URL || 'http://localhost:8080') {
    this.baseUrl = baseUrl;
  }

  /**
   * Initialize the API context.
   */
  async init(): Promise<void> {
    this.context = await request.newContext({
      baseURL: this.baseUrl,
    });
  }

  /**
   * Dispose of the API context.
   */
  async dispose(): Promise<void> {
    if (this.context) {
      await this.context.dispose();
      this.context = null;
    }
  }

  /**
   * Ensure context is initialized.
   */
  private ensureContext(): APIRequestContext {
    if (!this.context) {
      throw new Error('TestApiClient not initialized. Call init() first.');
    }
    return this.context;
  }

  /**
   * Check response status and throw if not ok.
   */
  private async checkResponse(response: any, endpoint: string): Promise<any> {
    if (!response.ok()) {
      const statusText = response.statusText();
      const status = response.status();
      let errorBody = '';
      try {
        errorBody = JSON.stringify(await response.json());
      } catch {
        errorBody = await response.text().catch(() => 'Unable to read response body');
      }
      throw new Error(
        `Test API request failed: ${endpoint} returned ${status} ${statusText}. Body: ${errorBody}`
      );
    }
    return response.json();
  }

  /**
   * Check if a user exists.
   */
  async userExists(email: string): Promise<UserExistsResponse> {
    const context = this.ensureContext();
    const response = await context.get(`/api/test/user/exists`, {
      params: { email },
    });
    return this.checkResponse(response, '/api/test/user/exists');
  }

  /**
   * Check if a user is enabled (verified).
   */
  async userEnabled(email: string): Promise<UserEnabledResponse> {
    const context = this.ensureContext();
    const response = await context.get(`/api/test/user/enabled`, {
      params: { email },
    });
    return this.checkResponse(response, '/api/test/user/enabled');
  }

  /**
   * Get user details.
   */
  async getUserDetails(email: string): Promise<UserDetailsResponse> {
    const context = this.ensureContext();
    const response = await context.get(`/api/test/user/details`, {
      params: { email },
    });
    return this.checkResponse(response, '/api/test/user/details');
  }

  /**
   * Get verification token for a user.
   */
  async getVerificationToken(email: string): Promise<VerificationTokenResponse> {
    const context = this.ensureContext();
    const response = await context.get(`/api/test/user/verification-token`, {
      params: { email },
    });
    return this.checkResponse(response, '/api/test/user/verification-token');
  }

  /**
   * Get password reset token for a user.
   */
  async getPasswordResetToken(email: string): Promise<PasswordResetTokenResponse> {
    const context = this.ensureContext();
    const response = await context.get(`/api/test/user/password-reset-token`, {
      params: { email },
    });
    return this.checkResponse(response, '/api/test/user/password-reset-token');
  }

  /**
   * Create a test user directly in the database.
   */
  async createUser(userData: CreateUserRequest): Promise<CreateUserResponse> {
    const context = this.ensureContext();
    const response = await context.post(`/api/test/user`, {
      data: userData,
    });
    return this.checkResponse(response, '/api/test/user');
  }

  /**
   * Delete a test user.
   */
  async deleteUser(email: string): Promise<DeleteUserResponse> {
    const context = this.ensureContext();
    const response = await context.delete(`/api/test/user`, {
      params: { email },
    });
    return this.checkResponse(response, '/api/test/user');
  }

  /**
   * Enable a user (simulate email verification).
   */
  async enableUser(email: string): Promise<EnableUserResponse> {
    const context = this.ensureContext();
    const response = await context.post(`/api/test/user/enable`, {
      params: { email },
    });
    return this.checkResponse(response, '/api/test/user/enable');
  }

  /**
   * Unlock a user account.
   */
  async unlockUser(email: string): Promise<UnlockUserResponse> {
    const context = this.ensureContext();
    const response = await context.post(`/api/test/user/unlock`, {
      params: { email },
    });
    return this.checkResponse(response, '/api/test/user/unlock');
  }

  /**
   * Create a verification token for a user.
   * Used to test email verification when emails are disabled.
   */
  async createVerificationToken(email: string): Promise<CreateVerificationTokenResponse> {
    const context = this.ensureContext();
    const response = await context.post(`/api/test/user/verification-token`, {
      params: { email },
    });
    return this.checkResponse(response, '/api/test/user/verification-token');
  }

  /**
   * Health check for Test API.
   */
  async health(): Promise<HealthResponse> {
    const context = this.ensureContext();
    const response = await context.get(`/api/test/health`);
    return this.checkResponse(response, '/api/test/health');
  }

  /**
   * Generate the verification URL for a user.
   */
  async getVerificationUrl(email: string): Promise<string | null> {
    const tokenResponse = await this.getVerificationToken(email);
    if (tokenResponse.token) {
      return `${this.baseUrl}/user/registrationConfirm?token=${tokenResponse.token}`;
    }
    return null;
  }

  /**
   * Generate the password reset URL for a user.
   */
  async getPasswordResetUrl(email: string): Promise<string | null> {
    const tokenResponse = await this.getPasswordResetToken(email);
    if (tokenResponse.token) {
      return `${this.baseUrl}/user/changePassword?token=${tokenResponse.token}`;
    }
    return null;
  }

  /**
   * Verify that a user was created with expected properties.
   */
  async verifyUserCreated(
    email: string,
    expectedFirstName: string,
    expectedLastName: string
  ): Promise<boolean> {
    const details = await this.getUserDetails(email);
    return (
      details.exists &&
      details.firstName === expectedFirstName &&
      details.lastName === expectedLastName
    );
  }

  /**
   * Verify that a user has been verified (enabled).
   */
  async verifyUserVerified(email: string): Promise<boolean> {
    const response = await this.userEnabled(email);
    return response.exists && response.enabled;
  }

  /**
   * Verify that a user has been deleted or no longer exists.
   */
  async verifyUserDeleted(email: string): Promise<boolean> {
    const response = await this.userExists(email);
    return !response.exists;
  }

  /**
   * Clean up a test user if it exists.
   */
  async cleanupUser(email: string): Promise<void> {
    const exists = await this.userExists(email);
    if (exists.exists) {
      await this.deleteUser(email);
    }
  }
}

/**
 * Create a singleton instance of the TestApiClient.
 */
let clientInstance: TestApiClient | null = null;

export async function getTestApiClient(): Promise<TestApiClient> {
  if (!clientInstance) {
    clientInstance = new TestApiClient();
    await clientInstance.init();
  }
  return clientInstance;
}

export async function disposeTestApiClient(): Promise<void> {
  if (clientInstance) {
    await clientInstance.dispose();
    clientInstance = null;
  }
}
