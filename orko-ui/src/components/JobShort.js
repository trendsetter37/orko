/*
 * Orko
 * Copyright © 2018-2019 Graham Crockford
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import React from "react"
import { Icon } from "semantic-ui-react"
import styled from "styled-components"

import Href from "./primitives/Href"
import Link from "./primitives/Link"
import * as jobTypes from "../services/jobTypes"

const BUY = "BUY"
const SELL = "SELL"

const JobShortBox = styled.div`
  padding-top: ${props => props.theme.space[1] + "px"};
  padding-bottom: ${props => props.theme.space[1] + "px"};
  border-bottom: 1px solid ${props => props.theme.colors.deemphasis};
  &:last-child {
    border: none;
  }
`

export default class JobShort extends React.Component {
  describe = job => {
    const ticker = t => t.exchange + ": " + t.base + "-" + t.counter

    if (job.jobType === jobTypes.OCO) {
      return (
        "On " +
        ticker(job.tickTrigger) +
        ": " +
        (job.high
          ? " If > " +
            job.high.thresholdAsString +
            " then " +
            this.describe(job.high.job) +
            "."
          : "") +
        (job.low
          ? " If < " +
            job.low.thresholdAsString +
            " then " +
            this.describe(job.low.job) +
            "."
          : "")
      )
    } else if (job.jobType === jobTypes.LIMIT_ORDER) {
      return (
        "On " + ticker(job.tickTrigger) + " " + job.direction + " " + job.amount
      )
    } else if (
      job.jobType === jobTypes.SOFT_TRAILING_STOP &&
      job.direction === SELL
    ) {
      return (
        "When " +
        ticker(job.tickTrigger) +
        " < " +
        job.stopPrice +
        " then sell at " +
        job.limitPrice +
        ". Highest bid price recorded is " +
        job.lastSyncPrice
      )
    } else if (
      job.jobType === jobTypes.SOFT_TRAILING_STOP &&
      job.direction === BUY
    ) {
      return (
        "When " +
        ticker(job.tickTrigger) +
        " > " +
        job.stopPrice +
        " then buy at " +
        job.limitPrice +
        ". Lowest ask price recorded is " +
        job.lastSyncPrice
      )
    } else if (job.jobType === jobTypes.ALERT) {
      return "Send alert '" + job.notification.message + "'"
    } else if (job.jobType === jobTypes.STATUS_UPDATE) {
      return "Status update"
    } else if (job.jobType === jobTypes.SCRIPT) {
      return job.name
    } else {
      return "Complex (" + job.jobType + ")"
    }
  }

  render() {
    return (
      <JobShortBox data-orko={"job/" + this.props.job.id}>
        <Href title="Delete job" onClick={this.props.onRemove}>
          <Icon name="close" />
        </Href>
        <Link to={"/job/" + this.props.job.id}>
          {this.describe(this.props.job)}
        </Link>
      </JobShortBox>
    )
  }
}
