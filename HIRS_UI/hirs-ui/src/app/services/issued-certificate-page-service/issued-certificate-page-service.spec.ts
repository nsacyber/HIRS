import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IssuedCertificatePageService } from './issued-certificate-page-service';

describe('IssuedCertificatePageService', () => {
  let service: IssuedCertificatePageService;

  beforeEach(() => {
        TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(IssuedCertificatePageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
