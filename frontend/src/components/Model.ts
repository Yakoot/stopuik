export interface UikMemberStatus {
  uik: string;
  tik: string;
  year: string;
  uik_status: string;
}

export interface UikCrimeLink {
  link_description: string;
  link: string;
}
export interface Crime {
  description: string;
  links: Array<UikCrimeLink>;
}

export interface SearchResult {
  filter_data: FilterData;
  name: string;
  status: Array<UikMemberStatus>;
  violations: {[key: string]: Array<Crime>};
  id: number;
}

export interface SearchQuery {
  ikmo?: string;
  report?: string;
  name?: string;
  year?: string;
  uik?: string;
  tik?: string;
}

export interface FilterData {
  description?: Array<string>;
  ikmo?: Array<string>;
  year?: Array<string>;
  uik?: Array<string>;
  tik?: Array<string>;
  report?: Array<string>;
}

export interface SearchResponse {
  data: Array<SearchResult>;
  filterData: FilterData;
}

export interface UikCrimeQuery {
  uik_member_id: number;
}

export interface UikCrimeResponse {
  violations: {[key: string]: Array<Crime>};
}
