import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DevicePageService } from './device-page-service';

describe('DevicePageService', () => {
  let service: DevicePageService;

  beforeEach(() => {
        TestBed.configureTestingModule({   
      providers: [
         provideHttpClient(),
         provideHttpClientTesting(),
      ]});
    service = TestBed.inject(DevicePageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
