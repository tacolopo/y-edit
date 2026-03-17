import { useState, useEffect, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { createToolFlow } from "@app/components/tools/shared/createToolFlow";
import { useCertSignParameters } from "@app/hooks/tools/certSign/useCertSignParameters";
import { useCertSignOperation } from "@app/hooks/tools/certSign/useCertSignOperation";
import { useBaseTool } from "@app/hooks/tools/shared/useBaseTool";
import { useAnnotation } from "@app/contexts/AnnotationContext";
import { BaseToolProps, ToolComponent } from "@app/types/tool";
import { Text, TextInput, Stack, Group, Button } from "@mantine/core";

interface DrawnRect {
  x: number;
  y: number;
  width: number;
  height: number;
  pageIndex: number;
  annotationId: string;
}

const CertSign = (props: BaseToolProps) => {
  const { t } = useTranslation();
  const { annotationApiRef } = useAnnotation();
  const [drawnRect, setDrawnRect] = useState<DrawnRect | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const unsubscribeRef = useRef<(() => void) | null>(null);

  const hasBox = drawnRect !== null;

  const cleanupAnnotation = useCallback(() => {
    if (!drawnRect || !annotationApiRef.current) return;
    const api = annotationApiRef.current;
    if (api.deleteAnnotation) {
      api.deleteAnnotation(drawnRect.pageIndex, drawnRect.annotationId);
    } else if (api.purgeAnnotation) {
      api.purgeAnnotation(drawnRect.pageIndex, drawnRect.annotationId);
    }
  }, [drawnRect, annotationApiRef]);

  const handleClear = useCallback(() => {
    cleanupAnnotation();
    setDrawnRect(null);
    setIsDrawing(false);
  }, [cleanupAnnotation]);

  const base = useBaseTool(
    'certSign',
    useCertSignParameters,
    useCertSignOperation,
    props
  );

  const handleDrawBox = useCallback(() => {
    const api = annotationApiRef.current;
    if (!api) return;

    // Clean up any existing drawn rectangle
    if (drawnRect) {
      cleanupAnnotation();
      setDrawnRect(null);
    }

    setIsDrawing(true);
    api.activateAnnotationTool('square', {
      strokeColor: '#2563eb',
      fillColor: '#2563eb',
      opacity: 0.15,
      borderWidth: 2,
    });

    // Unsubscribe any previous listener
    if (unsubscribeRef.current) {
      unsubscribeRef.current();
      unsubscribeRef.current = null;
    }

    if (api.onAnnotationEvent) {
      const unsub = api.onAnnotationEvent((event: any) => {
        const eventType = event?.type;
        if (
          eventType === 'create' ||
          eventType === 'created' ||
          eventType === 'annotationCreated' ||
          eventType === 'add' ||
          eventType === 'added' ||
          eventType === 'annotationAdded' ||
          eventType === 'complete'
        ) {
          const ann = event?.annotation ?? event?.selectedAnnotation;
          if (!ann) return;

          const annObject = ann?.object ?? ann;
          const rect = annObject?.rect;
          const id = annObject?.id ?? annObject?.uid ?? '';
          const pageIdx = annObject?.pageIndex ?? 0;

          if (rect) {
            const captured: DrawnRect = {
              x: Math.round(rect.origin.x),
              y: Math.round(rect.origin.y),
              width: Math.round(rect.size.width),
              height: Math.round(rect.size.height),
              pageIndex: pageIdx,
              annotationId: id,
            };
            setDrawnRect(captured);
            setIsDrawing(false);
            api.deactivateTools();

            // Unsubscribe after capturing
            if (unsubscribeRef.current) {
              unsubscribeRef.current();
              unsubscribeRef.current = null;
            }
          }
        }
      });
      if (typeof unsub === 'function') {
        unsubscribeRef.current = unsub;
      }
    }
  }, [annotationApiRef, drawnRect, cleanupAnnotation]);

  // Sync drawn rect coordinates to parameters
  useEffect(() => {
    if (drawnRect) {
      base.params.updateParameter('sigX', drawnRect.x);
      base.params.updateParameter('sigY', drawnRect.y);
      base.params.updateParameter('sigWidth', drawnRect.width);
      base.params.updateParameter('sigHeight', drawnRect.height);
      // pageIndex is 0-based, pageNumber is 1-based
      base.params.updateParameter('pageNumber', drawnRect.pageIndex + 1);
    } else {
      base.params.updateParameter('sigX', null);
      base.params.updateParameter('sigY', null);
      base.params.updateParameter('sigWidth', null);
      base.params.updateParameter('sigHeight', null);
    }
  }, [drawnRect]); // eslint-disable-line react-hooks/exhaustive-deps

  // Clean up the visual annotation after successful signing
  const prevHasResults = useRef(false);
  useEffect(() => {
    if (base.hasResults && !prevHasResults.current) {
      cleanupAnnotation();
      setDrawnRect(null);
    }
    prevHasResults.current = base.hasResults;
  }, [base.hasResults, cleanupAnnotation]);

  // Clean up event listener on unmount
  useEffect(() => {
    return () => {
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
      annotationApiRef.current?.deactivateTools();
    };
  }, [annotationApiRef]);

  return createToolFlow({
    forceStepNumbers: true,
    files: {
      selectedFiles: base.selectedFiles,
      isCollapsed: base.hasResults,
    },
    steps: [
      {
        title: t("certSign.placement.stepTitle", "Signature Placement"),
        isCollapsed: base.settingsCollapsed,
        onCollapsedClick: base.settingsCollapsed ? base.handleSettingsReset : undefined,
        content: (
          <Stack gap="md">
            <Text size="sm" c="dimmed">
              {t("certSign.placement.description", "Draw a rectangle on the PDF where you want the signature placed, then click Insert Signature. Your smart card PIN will be requested.")}
            </Text>

            {!hasBox && (
              <Button
                onClick={handleDrawBox}
                variant="light"
                loading={isDrawing}
              >
                {isDrawing
                  ? t("certSign.placement.drawing", "Draw on the PDF...")
                  : t("certSign.placement.drawBox", "Draw Signature Box")}
              </Button>
            )}

            {hasBox && (
              <Group gap="sm" align="center">
                <Text size="sm" fw={500}>
                  {t("certSign.placement.boxPlaced", "Signature box placed on page {{page}}", {
                    page: drawnRect.pageIndex + 1,
                  })}
                </Text>
                <Button variant="subtle" size="compact-sm" onClick={handleClear}>
                  {t("certSign.placement.clear", "Clear")}
                </Button>
              </Group>
            )}

            <TextInput
              label={t("certSign.details.reason", "Reason (optional)")}
              value={base.params.parameters.reason}
              onChange={(e) => base.params.updateParameter('reason', e.currentTarget.value)}
              placeholder={t("certSign.details.reasonPlaceholder", "Approved")}
            />
            <TextInput
              label={t("certSign.details.name", "Signer Name (optional)")}
              value={base.params.parameters.name}
              onChange={(e) => base.params.updateParameter('name', e.currentTarget.value)}
              placeholder={t("certSign.details.namePlaceholder", "Auto-detected from certificate")}
            />
          </Stack>
        ),
      },
    ],
    executeButton: {
      text: t("certSign.sign.submit", "Insert Signature"),
      isVisible: !base.hasResults,
      loadingText: t("loading"),
      onClick: base.handleExecute,
      disabled: !base.hasFiles || !base.endpointEnabled || !hasBox,
    },
    review: {
      isVisible: base.hasResults,
      operation: base.operation,
      title: t("certSign.sign.results", "Signed PDF"),
      onFileClick: base.handleThumbnailClick,
      onUndo: base.handleUndo,
    },
  });
};

export default CertSign as ToolComponent;
