import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IdevidCertificatePageService } from './idevid-certificate-page-service';

describe('IdevidCertificatePageService', () => {
  let service: IdevidCertificatePageService;

  beforeEach(() => {
        TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(IdevidCertificatePageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
