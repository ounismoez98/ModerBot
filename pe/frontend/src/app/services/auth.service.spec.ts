import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('restores the logged-in role from the server session', async () => {
    const restored = service.restoreSession();
    const request = http.expectOne('http://localhost:8080/api/auth/me');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ username: 'student', role: 'STUDENT' });

    await expect(restored).resolves.toBe('STUDENT');
    expect(service.username()).toBe('student');
  });

  it('clears local state after logging out from the server', async () => {
    service.acceptLogin('moderator', 'MODERATOR');
    const logout = service.logout();
    const request = http.expectOne('http://localhost:8080/api/auth/logout');
    expect(request.request.withCredentials).toBe(true);
    request.flush(null);

    await expect(logout).resolves.toBeUndefined();
    expect(service.role()).toBeNull();
    expect(service.username()).toBe('');
  });
});
