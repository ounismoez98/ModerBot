import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginPage } from './login';

describe('LoginPage', () => {
  let http: HttpTestingController;
  let navigation: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    navigation = vi.fn();
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigateByUrl: navigation } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('routes a student to the student home after authentication', () => {
    const fixture = TestBed.createComponent(LoginPage);
    const component = fixture.componentInstance;
    component.username.set('student');
    component.password.set('student-password');

    const submission = component.submit();

    const request = http.expectOne('http://localhost:8080/api/auth/login');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toEqual({ username: 'student', password: 'student-password' });
    request.flush({ username: 'student', role: 'STUDENT' });

    return submission.then(() => {
      expect(TestBed.inject(AuthService).role()).toBe('STUDENT');
      expect(navigation).toHaveBeenCalledWith('/home');
    });
  });

  it('routes a moderator to the moderation dashboard after authentication', () => {
    const fixture = TestBed.createComponent(LoginPage);
    const component = fixture.componentInstance;
    component.username.set('moderator');
    component.password.set('moderator-password');

    const submission = component.submit();

    const request = http.expectOne('http://localhost:8080/api/auth/login');
    request.flush({ username: 'moderator', role: 'MODERATOR' });

    return submission.then(() => {
      expect(TestBed.inject(AuthService).role()).toBe('MODERATOR');
      expect(navigation).toHaveBeenCalledWith('/moderation');
    });
  });
});
