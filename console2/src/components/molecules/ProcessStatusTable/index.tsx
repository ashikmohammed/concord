/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import * as React from 'react';
import { Link } from 'react-router-dom';
import { Button, Grid, Popup, Table } from 'semantic-ui-react';

import { getStatusSemanticColor, ProcessEntry } from '../../../api/process';
import { GitHubLink, LocalTimestamp } from '../index';

interface Props {
    data: ProcessEntry;
    onOpenWizard?: () => void;
    showStateDownload?: boolean;
    additionalActions?: React.ReactNode;
}

class ProcessStatusTable extends React.PureComponent<Props> {
    static renderCommitId(data: ProcessEntry) {
        if (!data.commitId) {
            return '-';
        }

        const link = (
            <GitHubLink
                url={data.repoUrl!}
                path={data.repoPath}
                commitId={data.commitId}
                text={data.commitId}
            />
        );
        if (!data.commitMsg) {
            return link;
        }

        return <Popup trigger={<span>{link}</span>} content={data.commitMsg} />;
    }

    render() {
        const { data, onOpenWizard, showStateDownload, additionalActions } = this.props;

        return (
            <Grid columns={2}>
                <Grid.Column>
                    <Table definition={true} color={getStatusSemanticColor(data.status)}>
                        <Table.Body>
                            <Table.Row>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Parent ID
                                </Table.Cell>
                                <Table.Cell>
                                    {data.parentInstanceId ? (
                                        <Link to={`/process/${data.parentInstanceId}`}>
                                            {data.parentInstanceId}
                                        </Link>
                                    ) : (
                                        ' - '
                                    )}
                                </Table.Cell>
                            </Table.Row>
                            <Table.Row>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Started by
                                </Table.Cell>
                                <Table.Cell>{data.initiator}</Table.Cell>
                            </Table.Row>
                            <Table.Row>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Created at
                                </Table.Cell>
                                <Table.Cell>
                                    <LocalTimestamp value={data.createdAt} />
                                </Table.Cell>
                            </Table.Row>
                            <Table.Row>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Last Update
                                </Table.Cell>
                                <Table.Cell>
                                    <LocalTimestamp value={data.lastUpdatedAt} />
                                </Table.Cell>
                            </Table.Row>
                            <Table.Row>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Actions
                                </Table.Cell>
                                <Table.Cell>
                                    <Button.Group>
                                        {onOpenWizard && (
                                            <Button
                                                onClick={() => onOpenWizard()}
                                                content="Wizard"
                                            />
                                        )}
                                        {showStateDownload && (
                                            <Button
                                                icon="download"
                                                color="blue"
                                                content="State"
                                                href={`/api/v1/process/${
                                                    data.instanceId
                                                }/state/snapshot`}
                                                download={`Concord_${data.status}_${
                                                    data.instanceId
                                                }.zip`}
                                            />
                                        )}
                                        {additionalActions}
                                    </Button.Group>
                                </Table.Cell>
                            </Table.Row>
                        </Table.Body>
                    </Table>
                </Grid.Column>
                <Grid.Column>
                    <Table
                        definition={true}
                        color={getStatusSemanticColor(data.status)}
                        style={{ height: '100%' }}>
                        <Table.Body style={{ wordBreak: 'break-all' }}>
                            <Table.Row>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Project
                                </Table.Cell>
                                <Table.Cell>
                                    {data.projectName ? (
                                        <Link
                                            to={`/org/${data.orgName}/project/${data.projectName}`}>
                                            {data.projectName}
                                        </Link>
                                    ) : (
                                        ' - '
                                    )}
                                </Table.Cell>
                            </Table.Row>
                            <Table.Row>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Repository
                                </Table.Cell>
                                <Table.Cell>{data.repoName ? data.repoName : ' - '}</Table.Cell>
                            </Table.Row>
                            <Table.Row>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Repository URL
                                </Table.Cell>
                                <Table.Cell>
                                    {data.repoUrl ? (
                                        <GitHubLink url={data.repoUrl} text={data.repoUrl} />
                                    ) : (
                                        ' - '
                                    )}
                                </Table.Cell>
                            </Table.Row>
                            <Table.Row>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Repository Path
                                </Table.Cell>
                                <Table.Cell>
                                    {data.repoPath ? (
                                        <GitHubLink
                                            url={data.repoUrl!}
                                            path={data.repoPath}
                                            text={data.repoPath}
                                        />
                                    ) : (
                                        ' - '
                                    )}
                                </Table.Cell>
                            </Table.Row>
                            <Table.Row style={{ height: '100%' }}>
                                <Table.Cell collapsing={true} singleLine={true}>
                                    Commit ID
                                </Table.Cell>
                                <Table.Cell>{ProcessStatusTable.renderCommitId(data)}</Table.Cell>
                            </Table.Row>
                        </Table.Body>
                    </Table>
                </Grid.Column>
            </Grid>
        );
    }
}

export default ProcessStatusTable;
