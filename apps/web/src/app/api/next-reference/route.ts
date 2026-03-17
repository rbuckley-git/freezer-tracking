import { NextResponse } from 'next/server';
import { nextReference } from '../../../lib/api';

export async function GET() {
  try {
    const result = await nextReference();
    return NextResponse.json(result);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unable to fetch next reference' },
      { status: 500 }
    );
  }
}
