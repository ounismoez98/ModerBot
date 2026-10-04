import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ModerationService } from './moderation.service';

describe('ModerationService', () => {
  let service: ModerationService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ModerationService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ModerationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('sends one comment to the individual moderation endpoint', () => {
    service.analyze('Comment about an exam').subscribe();

    const request = httpTesting.expectOne('http://localhost:8080/api/moderation/analyze');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ text: 'Comment about an exam' });
    request.flush({
      commentaire: 'Comment about an exam',
      scoreToxic: 0.1,
      scoreNonToxic: 0.9,
      sujet: 'examen',
      scoresSujets: { examen: 0.9 },
      decision: 'PUBLIER',
      erreur: null,
    });
  });

  it('sends comments as a batch', () => {
    service.analyzeBatch(['First comment', 'Second comment']).subscribe();

    const request = httpTesting.expectOne('http://localhost:8080/api/moderation/batch');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ commentaires: ['First comment', 'Second comment'] });
    request.flush([]);
  });
});
