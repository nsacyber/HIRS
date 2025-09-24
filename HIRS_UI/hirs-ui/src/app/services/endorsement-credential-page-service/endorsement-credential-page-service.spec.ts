import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EndorsementCredentialPageService } from './endorsement-credential-page-service';

describe('EndorsementCredentialPageService', () => {
  let service: EndorsementCredentialPageService;

  beforeEach(() => {
    TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(EndorsementCredentialPageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
