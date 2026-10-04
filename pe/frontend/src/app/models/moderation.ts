export type ModerationDecision = 'PUBLIER' | 'A_VERIFIER' | 'BLOQUER';

export interface ModerationResponse {
  commentaire: string;
  scoreToxic: number;
  scoreNonToxic: number;
  sujet: string;
  scoresSujets: Record<string, number>;
  decision: ModerationDecision;
  erreur: string | null;
  explication: string | null;
}

export interface CommentRequest {
  text?: string;
  commentaires?: string[];
}
