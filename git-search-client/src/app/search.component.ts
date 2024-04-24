import {Component, ViewEncapsulation } from '@angular/core';
import {HttpClient, HttpParams, HttpHeaders, HttpResponse, HttpErrorResponse} from '@angular/common/http';
import {first, map, tap} from 'rxjs/operators';
import { AppHighlightService } from './app.highlightService';
import stc from 'string-to-color';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css', './app.highlight.css'],
  providers: [HttpClient],
  encapsulation: ViewEncapsulation.None
})
export class SearchComponent {
  BASE_URL = 'http://localhost:8080';

  lastUpdateTime = null;

  searchRequest = null;
  currentSearchRequest = null;

  exactSearch = null;
  fuzzySearch = null;

  snippet = null;
  snippetAuthor = null;
  snippetDate = null;
  snippetPath = null;
  snippetLanguage = null;
  snippetBranch = null;
  snippetLink = null;

  selectedRow = null;
  selectedLineContent = null;
  selectedLineNumber = null;

  loadingResults = false;
  loadingSnippet = false;

  menuOpened = false;

  extensionFilter = localStorage.getItem('extensionFilter');
  packageFilter = localStorage.getItem('packageFilter');
  fileMaskFilter = localStorage.getItem('fileMaskFilter');
  directoryFilter = localStorage.getItem('directoryFilter');
  repositoryFilter = localStorage.getItem('repositoryFilter');
  branchFilter = localStorage.getItem('branchFilter');
  repositoryTypeFilter = localStorage.getItem('repositoryTypeFilter');

  repositoryTypeFilterOpened = false;

  constructor(private http: HttpClient, private appHighlightService: AppHighlightService) {}

  search(): void {
    if (this.currentSearchRequest !== null && this.currentSearchRequest.trim() !== '') {
      this.selectedRow = -1;
      this.snippet = [];
      this.snippetPath = null;
      this.snippetLink = null;
      this.searchRequest = this.currentSearchRequest.trim();

      let params = new HttpParams()
            .set('searchString', this.searchRequest);

      const storedExtensionFilter = localStorage.getItem('extensionFilter');
      const storedPackageFilter = localStorage.getItem('packageFilter');
      const storedFileMaskFilter = localStorage.getItem('fileMaskFilter');
      const storedDirectoryFilter = localStorage.getItem('directoryFilter');
      const storedRepositoryFilter = localStorage.getItem('repositoryFilter');
      const storedBranchFilter = localStorage.getItem('branchFilter');
      const storedRepositoryTypeFilter = localStorage.getItem('repositoryTypeFilter');

      if (storedExtensionFilter) {
        params = params.set('extensionFilter', storedExtensionFilter);
      }
      if (storedPackageFilter) {
        params = params.set('packageFilter', storedPackageFilter);
      }
      if (storedFileMaskFilter) {
        params = params.set('fileMaskFilter', storedFileMaskFilter);
      }
      if (storedDirectoryFilter) {
        params = params.set('directoryFilter', storedDirectoryFilter);
      }
      if (storedRepositoryFilter) {
        params = params.set('repositoryFilter', storedRepositoryFilter);
      }
      if (storedBranchFilter) {
        params = params.set('branchFilter', storedBranchFilter);
      }
      if (storedRepositoryTypeFilter && storedRepositoryTypeFilter !== 'any') {
        params = params.set('repositoryTypeFilter', storedRepositoryTypeFilter);
      }

      this.http.get<HttpResponse<any>>(this.BASE_URL + '/api/search', { params })
        .subscribe(
          (response: any) => {
            this.exactSearch = response.exactSearch;
            this.fuzzySearch = response.fuzzySearch;
            this.loadingResults = false;
          }
        );
      this.loadingResults = true;
      this.exactSearch = null;
      this.fuzzySearch = null;
    }
  }

  rowClick(index, lineContent, linePath, lineNumber, branch): void {
    this.selectedRow = index;
    this.snippetPath = linePath;
    this.selectedLineContent = lineContent;
    this.selectedLineNumber = lineNumber;

    if (this.filterIsPresent('branchFilter')) {
      branch = this.getFilter('branchFilter');
    }

    const params = new HttpParams()
          .set('linePath', linePath)
          .set('lineNumber', lineNumber)
          .set('branch', branch);

    this.http.get<HttpResponse<any>>(this.BASE_URL + '/api/search/snippet', { params })
      .subscribe(
        (response: any) => {
          this.snippet = response.snippet;
          this.snippetAuthor = response.authorEmail;
          this.snippetDate = response.date;
          this.snippetLanguage = this.getLanguage();
          this.snippetBranch = response.branch;
          this.loadingSnippet = false;
          this.snippetLink = response.link;
        }
      );
    this.loadingSnippet = true;
    this.snippet = null;
    this.snippetAuthor = null;
    this.snippetDate = null;
  }

  extractPath(path): string {
    const idx = path.lastIndexOf('/') + 1;
    return path.substring(idx);
  }

  printExactMatches(): string {
    const matchesCount = this.exactSearch.length;
    if (matchesCount === 1) {
      return matchesCount + ' result was found';
    } else {
      return matchesCount + ' results were found';
    }
  }

  printFuzzyMatches(): string {
      const matchesCount = this.fuzzySearch.length;
      if (matchesCount === 1) {
        return matchesCount + ' similar result was found';
      } else {
        return matchesCount + ' similar results were found';
      }
    }

  highlightCode(line, linePath): string {
    return this.appHighlightService.highlightCode(line, linePath, this.searchRequest);
  }

  inputIsEmpty(): boolean {
    return !this.currentSearchRequest || this.currentSearchRequest.length === 0;
  }

  clearInput(): void {
    this.currentSearchRequest = null;
  }

  fetch(): void {
    this.http.get<HttpResponse<any>>(this.BASE_URL + '/api/fetch')
          .subscribe();
  }

  noResultsFound(): boolean {
    const exactSearchNotFound = this.exactSearch != null && this.exactSearch.length === 0;
    const fuzzySearchNotFound = this.fuzzySearch != null && this.fuzzySearch.length === 0;

    return exactSearchNotFound && fuzzySearchNotFound;
  }

  getLanguage(): string {
    const extension = this.snippetPath.split('.').pop();
    let language = null;
    if (extension === 'kt') {
      language = 'kotlin';
    } else {
      language = extension;
    }
    return language;
  }

  getSnippetLanguageColor(): string {
    const color = stc(this.snippetLanguage);
    return color;
  }

  openMenu(): void {
    this.menuOpened = true;

    this.http.get<HttpResponse<any>>(this.BASE_URL + '/api/indexInfo')
        .subscribe(
          (response: any) => {
            this.lastUpdateTime = response.lastUpdateTime;
          }
        );
  }

  closeMenu(): void {
    this.menuOpened = false;
  }

  submitFilters(): void {
    this.clearLocalStorage();
    if (this.extensionFilter) {
      localStorage.setItem('extensionFilter', this.extensionFilter);
    }
    if (this.packageFilter) {
      localStorage.setItem('packageFilter', this.packageFilter);
    }
    if (this.fileMaskFilter) {
      localStorage.setItem('fileMaskFilter', this.fileMaskFilter);
    }
    if (this.directoryFilter) {
      localStorage.setItem('directoryFilter', this.directoryFilter);
    }
    if (this.repositoryFilter) {
      localStorage.setItem('repositoryFilter', this.repositoryFilter);
    }
    if (this.branchFilter) {
      localStorage.setItem('branchFilter', this.branchFilter);
    }
    if (this.repositoryTypeFilter) {
      localStorage.setItem('repositoryTypeFilter', this.repositoryTypeFilter);
    }
  }

  clearFilters(): void {
    this.extensionFilter = null;
    this.packageFilter = null;
    this.fileMaskFilter =  null;
    this.directoryFilter =  null;
    this.repositoryFilter =  null;
    this.branchFilter =  null;
    this.repositoryTypeFilter =  'any';

    this.clearLocalStorage();
  }

  clearLocalStorage(): void {
    localStorage.removeItem('extensionFilter');
    localStorage.removeItem('packageFilter');
    localStorage.removeItem('fileMaskFilter');
    localStorage.removeItem('directoryFilter');
    localStorage.removeItem('repositoryFilter');
    localStorage.removeItem('branchFilter');
    localStorage.setItem('repositoryTypeFilter', 'any');
  }

  getFilter(filter): string {
    return localStorage.getItem(filter);
  }

  filterIsPresent(filter): boolean {
    return localStorage.getItem(filter) !== null;
  }

  openRepositoryTypeFilterDropdown(): void {
    this.repositoryTypeFilterOpened = true;
  }

  selectRepositoryTypeFilter(value): void {
    this.repositoryTypeFilter = value;
    this.repositoryTypeFilterOpened = false;
  }
}
