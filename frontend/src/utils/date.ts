import dayjs from 'dayjs'

export function formatDate(date: string | Date, format = 'YYYY-MM-DD HH:mm'): string {
  return dayjs(date).format(format)
}

export function format_date_only(date: string | Date): string {
  return dayjs(date).format('YYYY-MM-DD')
}

export function isToday(date: string | Date): boolean {
  return dayjs(date).isSame(dayjs(), 'day')
}

export function getStartOfMonth(): string {
  return dayjs().startOf('month').format('YYYY-MM-DD')
}

export function getEndOfMonth(): string {
  return dayjs().endOf('month').format('YYYY-MM-DD')
}

export function addDays(date: string | Date, days: number): string {
  return dayjs(date).add(days, 'day').format('YYYY-MM-DD')
}

export function getDateRangeForMonth(year: number, month: number): { start: string; end: string } {
  const start = dayjs().year(year).month(month - 1).startOf('month')
  const end = dayjs().year(year).month(month - 1).endOf('month')
  return {
    start: start.format('YYYY-MM-DD'),
    end: end.format('YYYY-MM-DD')
  }
}
