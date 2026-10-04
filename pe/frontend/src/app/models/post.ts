export interface Post {
  id: number;
  title: string;
  content: string;
  author: string;
  createdAt: string;
  status: 'PENDING' | 'PUBLISHED' | 'REJECTED';
  toxicityScore: number | null;
  suggestedDecision: 'PUBLIER' | 'A_VERIFIER' | 'BLOQUER' | null;
  suggestedSubject: string | null;
  suggestedExplanation: string | null;
  moderationReason: string | null;
  moderatedBy: string | null;
  moderatedAt: string | null;
}

export interface PostRequest {
  title: string;
  content: string;
  author: string;
}
