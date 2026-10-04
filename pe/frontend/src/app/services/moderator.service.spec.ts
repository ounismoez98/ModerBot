import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ModeratorService } from './moderator.service';

describe('ModeratorService', () => {
  let service: ModeratorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ModeratorService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ModeratorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads moderation statistics with the current session', () => {
    service.getStatistics().subscribe();
    const request = http.expectOne('http://localhost:8080/api/moderation/queue/statistics');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ pending: 2, published: 4, rejected: 1 });
  });

  it('sends a reason with a moderator decision', () => {
    service.publish(12, 'Le message répond aux règles.').subscribe();
    const request = http.expectOne('http://localhost:8080/api/moderation/queue/12/publish');
    expect(request.request.body).toEqual({ reason: 'Le message répond aux règles.' });
    expect(request.request.withCredentials).toBe(true);
    request.flush({});
  });
});
