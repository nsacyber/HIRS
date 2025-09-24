import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ReferenceManifestPageService } from './reference-manifest-page-service';

describe('ReferenceManifestPageService', () => {
  let service: ReferenceManifestPageService;

  beforeEach(() => {
        TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(ReferenceManifestPageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
