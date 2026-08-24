import type { ReactNode } from 'react'

export function SettingsRow(props: {
  label: string
  title?: string
  column?: boolean
  children: ReactNode
}) {
  return (
    <label
      className={props.column ? 'flex flex-col gap-1' : 'flex items-center justify-between gap-2'}
      title={props.title}
    >
      {props.label}
      {props.children}
    </label>
  )
}

export function SettingsFieldSelect(props: {
  value: string | number
  onChange: (value: string) => void
  children: ReactNode
}) {
  return (
    <select
      value={props.value}
      onChange={(e) => props.onChange(e.target.value)}
      className="rounded border border-[var(--cc-border)] cc-native-field px-1"
    >
      {props.children}
    </select>
  )
}

export function SettingsNumber(props: {
  value: number
  min?: number
  max?: number
  onChange: (value: number) => void
}) {
  return (
    <input
      type="number"
      min={props.min}
      max={props.max}
      className="w-16 rounded border border-[var(--cc-border)] cc-native-field px-1"
      value={props.value}
      onChange={(e) => props.onChange(Number(e.target.value))}
    />
  )
}
