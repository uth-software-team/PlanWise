import { AvailabilityRulesSection } from './AvailabilityRulesSection'
import { FixedEventsSection } from './FixedEventsSection'

export function AvailabilitySettingsPanel() {
  return (
    <section
      id="availability"
      aria-label="Thời gian rảnh và lịch cố định"
      className="mt-8 space-y-6"
    >
      <AvailabilityRulesSection />
      <FixedEventsSection />
    </section>
  )
}
