export interface UikMemberStatus {
  uik: number;
  tik: number;
  year: number;
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

export type UikType = "UIK" | "TIK" | "IKMO";

export interface AllUiksQuery {
  year: number;
}

export interface AllUiksResponseItem {
  name: string;
  id: number;
  type: UikType;
  managingUikId: number;
}

export interface AllUiksResponse {
  uiks: Array<AllUiksResponseItem>
}
export interface UikMembersQuery {
  uik: number;
  year: number;
}

export interface UikMemberDto {
  id: number;
  name: string;
  status: number;
}

export interface UikMembersResponse {
  people: Array<UikMemberDto>
}

interface CreateCrimeRequestLinkItem {
  title: string;
  url: string;
}

export interface CreateCrimeRequest {
  uik: number;
  uikMembers: Array<number>;
  newUikMembers: Array<UikMemberDto>;
  crimeType: string;
  crimeLinks: Array<CreateCrimeRequestLinkItem>;
}

export interface CreateCrimeResponse {
  crimeId: number;
  message: string;
}

export interface TimelineResponseItem {
  year: number;
  title: string;
  date: string;
  crimeCount: number;
  uikMemberCount: number;
}

export interface TimelineResponse {
  elections: Array<TimelineResponseItem>
}

export function formatUikLabel(uik: AllUiksResponseItem | undefined): string {
  if (uik === undefined) {
    return "";
  }
  if (uik.id > 0) {
    return `УИК ${uik.name}`;
  }
  if (uik.id < -100) {
    return `ИКМО ${uik.name}`;
  }
  return uik.name;

}

export function formatStatus(status: number): string {
  switch (status) {
    case 1: return "Пред.";
    case 2: return "Зам.";
    case 3: return "Секр.";
    case 4: return "ЧПРГ";
    case 5: return "ЧПСГ";
    default: return `[${status}]`;
  }
}

