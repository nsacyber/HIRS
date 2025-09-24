import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TrustChainCertificatePageService } from './trust-chain-certificate-page-service';

describe('TrustChainCertificatePageService', () => {
  let service: TrustChainCertificatePageService;

  beforeEach(() => {
        TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(TrustChainCertificatePageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
